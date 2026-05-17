import pandas as pd
import plotly.express as px
import plotly.io as pio
from sqlalchemy import create_engine
import json
import os

def generate_dashboard():
    print("Connexion à la base de données MySQL...")
    try:
        # Configuration de la connexion avec SQLAlchemy et PyMySQL
        engine = create_engine('mysql+pymysql://root:@localhost/cinescoop')
        
        print("Extraction des données globales...")
        df_movies = pd.read_sql("SELECT * FROM movies", engine)
        df_users = pd.read_sql("SELECT * FROM users", engine)
        df_ratings = pd.read_sql("SELECT * FROM ratings", engine)
        
        # Requête complète associant Acteurs, Rôles et caractéristiques des films
        df_cast = pd.read_sql("""
            SELECT 
                a.actor_id,
                a.full_name AS actor_name,
                a.nationality,
                a.awards_count,
                a.image_url,
                ma.role_name,
                ma.screen_time,
                m.title AS movie_title,
                m.release_year,
                m.budget,
                m.revenue
            FROM actors a
            JOIN movie_actors ma ON a.actor_id = ma.actor_id
            JOIN movies m ON ma.movie_id = m.movie_id
        """, engine)

    except Exception as e:
        print(f"Erreur lors de la récupération des données : {e}")
        return

    print("Calcul des KPIs globaux...")
    total_movies = len(df_movies)
    avg_rating = df_movies['rating_average'].mean()
    total_revenue = df_movies['revenue'].sum()
    total_users = len(df_users)
    total_ratings = len(df_ratings)
    avg_watch_time = df_users['watch_time'].mean() if 'watch_time' in df_users.columns else 0

    # Formatage des KPIs
    revenue_str = f"{total_revenue / 1e9:.2f} Md" if pd.notnull(total_revenue) else "0"
    avg_rating_str = f"{avg_rating:.2f}" if pd.notnull(avg_rating) else "0.00"
    watch_time_str = f"{avg_watch_time / 1000:.2f} K" if pd.notnull(avg_watch_time) else "0"

    print("Génération des graphiques globaux avec Plotly (Mode Clair, Onglet 1)...")
    template = 'plotly_white'

    # --- 1. Graphique: Genres ---
    genre_counts = df_movies['genre'].value_counts().reset_index()
    genre_counts.columns = ['genre', 'count']
    fig_genre = px.pie(genre_counts, values='count', names='genre', title='Répartition par genre', 
                       template=template, color_discrete_sequence=px.colors.qualitative.Safe)
    fig_genre.update_layout(
        paper_bgcolor='rgba(0,0,0,0)', 
        plot_bgcolor='rgba(0,0,0,0)', 
        font=dict(color='#1f2937', family='Inter, sans-serif'),
        margin=dict(t=40, b=20, l=20, r=20)
    )

    # --- 2. Graphique: Utilisateurs par Pays ---
    if 'country' in df_users.columns:
        country_counts = df_users['country'].value_counts().reset_index()
        country_counts.columns = ['country', 'count']
        fig_country = px.pie(country_counts, values='count', names='country', hole=0.4, 
                             title='Utilisateurs par pays', template=template,
                             color_discrete_sequence=px.colors.qualitative.Pastel)
        fig_country.update_layout(
            paper_bgcolor='rgba(0,0,0,0)', 
            plot_bgcolor='rgba(0,0,0,0)', 
            font=dict(color='#1f2937', family='Inter, sans-serif'),
            margin=dict(t=40, b=20, l=20, r=20)
        )
    else:
        fig_country = px.pie(title='Données pays indisponibles')

    # --- 3. Graphique: Sorties par Année ---
    yearly_movies = df_movies.groupby('release_year').size().reset_index(name='count')
    yearly_movies = yearly_movies[yearly_movies['release_year'] > 1800] 
    fig_year = px.line(yearly_movies, x='release_year', y='count', title='Films par année de sortie', template=template)
    fig_year.update_traces(line_color='#6366f1', fill='tozeroy', fillcolor='rgba(99, 102, 241, 0.1)', line_width=3) 
    fig_year.update_layout(
        paper_bgcolor='rgba(0,0,0,0)', 
        plot_bgcolor='rgba(0,0,0,0)', 
        font=dict(color='#1f2937', family='Inter, sans-serif'),
        xaxis=dict(gridcolor='rgba(0,0,0,0.05)'),
        yaxis=dict(gridcolor='rgba(0,0,0,0.05)'),
        margin=dict(t=40, b=20, l=20, r=20)
    )

    # --- 4. Graphique: Top 10 Films (Filtre Interactif) ---
    import plotly.graph_objects as go
    top_revenue = df_movies.nlargest(10, 'revenue').sort_values('revenue', ascending=True)
    
    # We use a fallback if budget is not there, but it should be
    has_budget = 'budget' in df_movies.columns
    top_budget = df_movies.nlargest(10, 'budget').sort_values('budget', ascending=True) if has_budget else top_revenue

    fig_revenue = go.Figure()

    # Trace 1: Revenus
    fig_revenue.add_trace(go.Bar(
        x=top_revenue['revenue'],
        y=top_revenue['title'],
        orientation='h',
        name='Revenus',
        marker_color='#4f46e5',
        visible=True
    ))

    # Trace 2: Budget
    fig_revenue.add_trace(go.Bar(
        x=top_budget['budget'] if has_budget else top_revenue['revenue'],
        y=top_budget['title'] if has_budget else top_revenue['title'],
        orientation='h',
        name='Budget',
        marker_color='#10b981',
        visible=False
    ))

    # Trace 3: Popularité (Vote Average)
    has_vote = 'vote_average' in df_movies.columns
    top_vote = df_movies.nlargest(10, 'vote_average').sort_values('vote_average', ascending=True) if has_vote else top_revenue
    fig_revenue.add_trace(go.Bar(
        x=top_vote['vote_average'] if has_vote else top_revenue['revenue'],
        y=top_vote['title'] if has_vote else top_revenue['title'],
        orientation='h',
        name='Note Moyenne',
        marker_color='#f59e0b',
        visible=False
    ))

    # Ajout du filtre (updatemenus) pour basculer entre les vues
    fig_revenue.update_layout(
        title='Top 10 Films par Revenus',
        updatemenus=[
            dict(
                type="dropdown",
                direction="down",
                active=0,
                x=0.99,
                y=1.18,
                xanchor="right",
                yanchor="top",
                buttons=list([
                    dict(label="💰 Filtrer par Revenus",
                         method="update",
                         args=[{"visible": [True, False, False]},
                               {"title": "Top 10 Films par Revenus"}]),
                    dict(label="📈 Filtrer par Budget",
                         method="update",
                         args=[{"visible": [False, True, False]},
                               {"title": "Top 10 Films par Budget"}]),
                    dict(label="⭐ Filtrer par Note (Populaires)",
                         method="update",
                         args=[{"visible": [False, False, True]},
                               {"title": "Top 10 Films les Mieux Notés"}]),
                ]),
                pad={"r": 10, "t": 10},
                showactive=True,
                bgcolor="#ffffff",
                bordercolor="#d1d5db",
                borderwidth=1
            )
        ],
        paper_bgcolor='rgba(0,0,0,0)', 
        plot_bgcolor='rgba(0,0,0,0)', 
        font=dict(color='#1f2937', family='Inter, sans-serif'),
        xaxis=dict(gridcolor='rgba(0,0,0,0.05)'),
        margin=dict(t=60, b=20, l=20, r=20)
    )

    # Conversion en div HTML
    div_genre = pio.to_html(fig_genre, full_html=False, include_plotlyjs='cdn')
    div_country = pio.to_html(fig_country, full_html=False, include_plotlyjs=False)
    div_year = pio.to_html(fig_year, full_html=False, include_plotlyjs=False)
    div_revenue = pio.to_html(fig_revenue, full_html=False, include_plotlyjs=False)

    print("Agrégation et modélisation des profils d'Acteurs (Onglet 2)...")
    actor_groups = df_cast.groupby('actor_id')
    actor_data = {}

    for actor_id, group in actor_groups:
        first_row = group.iloc[0]
        actor_name = first_row['actor_name']
        nationality = first_row['nationality'] if pd.notnull(first_row['nationality']) else "Inconnue"
        awards = int(first_row['awards_count']) if pd.notnull(first_row['awards_count']) else 0
        image_url = first_row['image_url'] if pd.notnull(first_row['image_url']) else ""
        
        total_screen_time = int(group['screen_time'].sum())
        movie_count = len(group)
        avg_budget = float(group['budget'].mean()) if pd.notnull(group['budget'].mean()) else 0.0
        avg_revenue = float(group['revenue'].mean()) if pd.notnull(group['revenue'].mean()) else 0.0
        
        movies_list = []
        for _, row in group.iterrows():
            movies_list.append({
                'title': row['movie_title'],
                'role': row['role_name'] if pd.notnull(row['role_name']) else "Rôle principal",
                'screen_time': int(row['screen_time']) if pd.notnull(row['screen_time']) else 0,
                'year': int(row['release_year']) if pd.notnull(row['release_year']) else 0,
                'budget': float(row['budget']) if pd.notnull(row['budget']) else 0.0,
                'revenue': float(row['revenue']) if pd.notnull(row['revenue']) else 0.0
            })
        
        actor_data[str(actor_id)] = {
            'id': int(actor_id),
            'name': actor_name,
            'nationality': nationality,
            'awards': awards,
            'image_url': image_url,
            'total_screen_time': total_screen_time,
            'movie_count': movie_count,
            'avg_budget': avg_budget,
            'avg_revenue': avg_revenue,
            'movies': movies_list
        }

    # Extraction des 6 stars majeures par nombre de films pour la rangée interactive du haut
    top_actors_df = df_cast.groupby('actor_id').agg({
        'actor_name': 'first',
        'nationality': 'first',
        'awards_count': 'first',
        'image_url': 'first',
        'movie_title': 'count'
    }).rename(columns={'movie_title': 'movies_count'}).nlargest(6, 'movies_count').reset_index()

    top_actors_list = []
    for _, row in top_actors_df.iterrows():
        top_actors_list.append({
            'id': int(row['actor_id']),
            'name': row['actor_name'],
            'nationality': row['nationality'] if pd.notnull(row['nationality']) else "Inconnue",
            'image_url': row['image_url'] if pd.notnull(row['image_url']) else "",
            'movies_count': int(row['movies_count'])
        })

    actor_json = json.dumps(actor_data, ensure_ascii=False)
    top_actors_json = json.dumps(top_actors_list, ensure_ascii=False)

    print("Génération de la page web premium interactive en Mode Clair à double onglet...")
    html_content = f"""
    <!DOCTYPE html>
    <html lang="fr">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CineScoop Portal - Hub de Visualisation Premium</title>
        <!-- Google Fonts -->
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
        <style>
            body {{
                background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
                background-attachment: fixed;
                color: #1f2937;
                font-family: 'Inter', system-ui, -apple-system, sans-serif;
                margin: 0;
                padding: 24px;
                min-height: 100vh;
            }}
            .header-title {{
                text-align: center;
                margin-bottom: 30px;
                animation: slideDown 0.6s cubic-bezier(0.16, 1, 0.3, 1);
            }}
            .header-title h1 {{
                font-size: 3.2em;
                font-weight: 800;
                background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                margin: 0 0 5px 0;
                letter-spacing: -1.5px;
            }}
            .header-title p {{
                color: #4b5563;
                font-size: 1.1em;
                font-weight: 500;
                margin: 0;
            }}

            /* Onglets / Tabs */
            .nav-tabs {{
                display: flex;
                justify-content: center;
                gap: 15px;
                margin-bottom: 30px;
                border-bottom: 2px solid rgba(0, 0, 0, 0.05);
                padding-bottom: 12px;
                animation: fadeIn 0.8s ease;
            }}
            .tab-btn {{
                background: none;
                border: none;
                color: #6b7280;
                font-size: 1.1em;
                font-weight: 700;
                padding: 10px 24px;
                cursor: pointer;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                border-radius: 8px;
            }}
            .tab-btn:hover {{
                color: #111827;
                background-color: rgba(0, 0, 0, 0.03);
                transform: translateY(-1px);
            }}
            .tab-btn.active {{
                color: white;
                background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
                box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
            }}

            .tab-content {{
                display: none;
                animation: scaleUp 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            }}
            .tab-content.active {{
                display: block;
            }}

            /* Onglet 1 : Tableau de Bord KPIs */
            .kpi-row {{
                display: grid;
                grid-template-columns: repeat(6, 1fr);
                gap: 15px;
                margin-bottom: 30px;
            }}
            .kpi-card {{
                background: rgba(255, 255, 255, 0.85);
                border: 1px solid rgba(255, 255, 255, 0.7);
                border-radius: 16px;
                padding: 20px 15px;
                text-align: center;
                backdrop-filter: blur(10px);
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
                transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
                border-top: 4px solid #3b82f6;
            }}
            .kpi-card:nth-child(1) {{ border-top-color: #3b82f6; }}
            .kpi-card:nth-child(2) {{ border-top-color: #10b981; }}
            .kpi-card:nth-child(3) {{ border-top-color: #f59e0b; }}
            .kpi-card:nth-child(4) {{ border-top-color: #8b5cf6; }}
            .kpi-card:nth-child(5) {{ border-top-color: #ec4899; }}
            .kpi-card:nth-child(6) {{ border-top-color: #06b6d4; }}

            .kpi-card:hover {{
                transform: translateY(-6px) scale(1.02);
                box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
                background: rgba(255, 255, 255, 0.95);
            }}
            .kpi-value {{
                font-size: 2.2em;
                font-weight: 800;
                color: #1f2937;
                margin-bottom: 5px;
            }}
            .kpi-label {{
                font-size: 0.8em;
                color: #6b7280;
                font-weight: bold;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }}

            .charts-grid {{
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 25px;
            }}
            .chart-wrapper {{
                background: rgba(255, 255, 255, 0.85);
                border: 1px solid rgba(255, 255, 255, 0.7);
                border-radius: 20px;
                padding: 15px;
                backdrop-filter: blur(10px);
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
                transition: all 0.3s ease;
            }}
            .chart-wrapper:hover {{
                box-shadow: 0 12px 30px rgba(0, 0, 0, 0.07);
                border-color: rgba(59, 130, 246, 0.3);
            }}

            /* Onglet 2 : Casting Explorer */
            .casting-container {{
                background: rgba(255, 255, 255, 0.85);
                border: 1px solid rgba(255, 255, 255, 0.7);
                border-radius: 24px;
                padding: 35px;
                backdrop-filter: blur(12px);
                box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
            }}
            .casting-header {{
                text-align: center;
                margin-bottom: 30px;
            }}
            .casting-header h2 {{
                font-size: 2.2em;
                font-weight: 800;
                color: #111827;
                margin: 0 0 8px 0;
            }}
            .casting-header p {{
                color: #4b5563;
                font-size: 1.05em;
                margin: 0;
            }}

            /* Top Star Bubbles Interactive Bar */
            .top-stars-row {{
                display: flex;
                justify-content: center;
                gap: 15px;
                margin: 25px 0 35px 0;
                overflow-x: auto;
                padding: 5px;
            }}
            .star-bubble {{
                background: white;
                border: 1px solid rgba(0,0,0,0.06);
                border-radius: 50px;
                padding: 8px 18px;
                display: flex;
                align-items: center;
                gap: 10px;
                cursor: pointer;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                box-shadow: 0 4px 10px rgba(0, 0, 0, 0.03);
                white-space: nowrap;
            }}
            .star-bubble:hover {{
                transform: translateY(-3px) scale(1.05);
                background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
                color: white;
                border-color: transparent;
                box-shadow: 0 8px 20px rgba(59, 130, 246, 0.25);
            }}
            .star-bubble:hover div {{
                color: inherit !important;
            }}
            .star-bubble:hover .bubble-avatar {{
                background: white;
                color: #3b82f6;
            }}
            .bubble-avatar {{
                width: 32px;
                height: 32px;
                background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 800;
                font-size: 0.85em;
                color: white;
                transition: all 0.3s;
                overflow: hidden;
            }}

            /* Dynamic search selection */
            .selector-box {{
                display: flex;
                justify-content: center;
                align-items: center;
                gap: 20px;
                flex-wrap: wrap;
                margin-top: 10px;
            }}
            .search-wrapper {{
                position: relative;
                width: 320px;
            }}
            .search-input {{
                width: 100%;
                padding: 12px 20px 12px 40px;
                font-size: 1.05em;
                border-radius: 30px;
                border: 2px solid rgba(0, 0, 0, 0.08);
                background: white url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="gray" class="bi bi-search" viewBox="0 0 16 16"><path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/></svg>') no-repeat 15px center;
                outline: none;
                transition: all 0.3s;
                box-shadow: 0 4px 10px rgba(0, 0, 0, 0.02);
            }}
            .search-input:focus {{
                border-color: #3b82f6;
                box-shadow: 0 0 15px rgba(59, 130, 246, 0.15);
            }}
            .search-suggestions {{
                position: absolute;
                top: 100%;
                left: 0;
                right: 0;
                background: white;
                border-radius: 12px;
                margin-top: 8px;
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
                max-height: 250px;
                overflow-y: auto;
                z-index: 10;
                display: none;
                border: 1px solid rgba(0, 0, 0, 0.05);
            }}
            .suggestion-item {{
                padding: 10px 20px;
                cursor: pointer;
                transition: background 0.2s;
                font-weight: 500;
                text-align: left;
            }}
            .suggestion-item:hover {{
                background-color: #f3f4f6;
                color: #3b82f6;
            }}

            .selector-box select {{
                background-color: white;
                color: #1f2937;
                border: 2px solid rgba(0, 0, 0, 0.08);
                border-radius: 30px;
                padding: 12px 24px;
                font-size: 1.05em;
                outline: none;
                cursor: pointer;
                transition: all 0.3s;
            }}
            .selector-box select:focus {{
                border-color: #3b82f6;
                box-shadow: 0 0 15px rgba(59, 130, 246, 0.15);
            }}

            /* Actor profile and animations */
            .actor-profile {{
                animation: scaleUp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
            }}
            .actor-hero {{
                display: flex;
                align-items: center;
                gap: 25px;
                margin-bottom: 30px;
                border-bottom: 2px solid rgba(0, 0, 0, 0.05);
                padding-bottom: 25px;
            }}
            .actor-avatar {{
                width: 95px;
                height: 95px;
                background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
                border-radius: 50%;
                display: flex;
                justify-content: center;
                align-items: center;
                font-size: 2.4em;
                font-weight: 800;
                color: white;
                box-shadow: 0 8px 20px rgba(59, 130, 246, 0.2);
                animation: pulseAvatar 2s infinite alternate;
                overflow: hidden;
            }}
            .actor-details h3 {{
                font-size: 2.4em;
                font-weight: 800;
                color: #111827;
                margin: 0 0 8px 0;
            }}
            .actor-badges {{
                display: flex;
                gap: 10px;
            }}
            .badge {{
                background-color: #f3f4f6;
                color: #4b5563;
                padding: 6px 14px;
                border-radius: 20px;
                font-size: 0.9em;
                font-weight: bold;
            }}
            .badge-gold {{
                background-color: rgba(245, 158, 11, 0.1);
                color: #d97706;
                border: 1px solid rgba(245, 158, 11, 0.2);
            }}

            .actor-stats-grid {{
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 20px;
                margin-bottom: 40px;
            }}
            .actor-stat-card {{
                background: white;
                border: 1px solid rgba(0, 0, 0, 0.05);
                border-radius: 16px;
                padding: 20px;
                text-align: center;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            }}
            .actor-stat-card:hover {{
                transform: translateY(-4px);
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
                border-color: rgba(59, 130, 246, 0.2);
            }}
            .actor-stat-val {{
                font-size: 2.2em;
                font-weight: 800;
                color: #3b82f6;
                margin-bottom: 5px;
            }}
            .actor-stat-lbl {{
                font-size: 0.85em;
                color: #6b7280;
                text-transform: uppercase;
                font-weight: bold;
                letter-spacing: 0.5px;
            }}

            .section-title {{
                font-size: 1.6em;
                font-weight: 800;
                color: #111827;
                margin-bottom: 20px;
                border-left: 5px solid #3b82f6;
                padding-left: 15px;
            }}
            .filmography-grid {{
                display: grid;
                grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
                gap: 25px;
            }}
            .film-card {{
                background: white;
                border: 1px solid rgba(0, 0, 0, 0.05);
                border-radius: 16px;
                padding: 24px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.02);
            }}
            .film-card:hover {{
                transform: translateY(-5px);
                border-color: rgba(59, 130, 246, 0.3);
                box-shadow: 0 12px 28px rgba(59, 130, 246, 0.08);
            }}
            .film-header {{
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 15px;
            }}
            .film-header h5 {{
                font-size: 1.25em;
                font-weight: 700;
                color: #111827;
                margin: 0;
            }}
            .film-year {{
                background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
                color: white;
                padding: 4px 10px;
                border-radius: 20px;
                font-size: 0.8em;
                font-weight: bold;
                box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
            }}
            .film-row {{
                margin-bottom: 8px;
                font-size: 0.95em;
                color: #4b5563;
            }}
            .role-badge {{
                background-color: rgba(139, 92, 246, 0.1);
                color: #7c3aed;
                padding: 3px 10px;
                border-radius: 20px;
                font-weight: bold;
                font-size: 0.9em;
            }}
            .film-financials {{
                margin-top: 15px;
                display: flex;
                flex-direction: column;
                gap: 8px;
            }}
            .fin-badge {{
                padding: 6px 12px;
                border-radius: 8px;
                font-size: 0.85em;
                font-weight: bold;
            }}
            .fin-budget {{
                background-color: rgba(16, 185, 129, 0.08);
                color: #059669;
                border: 1px solid rgba(16, 185, 129, 0.15);
            }}
            .fin-revenue {{
                background-color: rgba(6, 182, 212, 0.08);
                color: #0891b2;
                border: 1px solid rgba(6, 182, 212, 0.15);
            }}

            .empty-state {{
                text-align: center;
                padding: 60px;
                color: #6b7280;
            }}
            .empty-icon {{
                font-size: 4em;
                margin-bottom: 15px;
                animation: bounceEmpty 2s infinite alternate;
            }}

            /* Animations Definitions */
            @keyframes fadeIn {{
                from {{ opacity: 0; }}
                to {{ opacity: 1; }}
            }}
            @keyframes slideDown {{
                from {{ opacity: 0; transform: translateY(-30px); }}
                to {{ opacity: 1; transform: translateY(0); }}
            }}
            @keyframes scaleUp {{
                from {{ opacity: 0; transform: scale(0.95); }}
                to {{ opacity: 1; transform: scale(1); }}
            }}
            @keyframes pulseAvatar {{
                from {{ box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }}
                to {{ box-shadow: 0 0 0 10px rgba(59, 130, 246, 0); }}
            }}
            @keyframes bounceEmpty {{
                from {{ transform: translateY(0); }}
                to {{ transform: translateY(-10px); }}
            }}

            /* Responsive */
            @media (max-width: 1024px) {{
                .charts-grid {{ grid-template-columns: 1fr; }}
                .kpi-row {{ grid-template-columns: repeat(3, 1fr); }}
                .actor-stats-grid {{ grid-template-columns: repeat(2, 1fr); }}
            }}
            @media (max-width: 640px) {{
                .kpi-row {{ grid-template-columns: repeat(2, 1fr); }}
                .actor-stats-grid {{ grid-template-columns: 1fr; }}
            }}
        </style>
    </head>
    <body>
        <div class="header-title">
            <h1>🎬 CINESCOOP PORTAL</h1>
            <p>Hub Analytique Intelligent & Explorateur Interactif de Casting</p>
        </div>

        <!-- Navigation par Onglets -->
        <div class="nav-tabs">
            <button class="tab-btn active" onclick="switchTab('dashboard')">📈 Tableau de Bord</button>
            <button class="tab-btn" onclick="switchTab('casting')">🎭 Casting Network Explorer</button>
        </div>

        <!-- CONTENU ONGLET 1 : TABLEAU DE BORD (MODE CLAIR) -->
        <div id="dashboard-tab" class="tab-content active">
            <div class="kpi-row">
                <div class="kpi-card"><div class="kpi-value">{total_users}</div><div class="kpi-label">Utilisateurs</div></div>
                <div class="kpi-card"><div class="kpi-value">{total_ratings}</div><div class="kpi-label">Évaluations</div></div>
                <div class="kpi-card"><div class="kpi-value">{watch_time_str}</div><div class="kpi-label">watch_time Moyen</div></div>
                <div class="kpi-card"><div class="kpi-value">{total_movies}</div><div class="kpi-label">Nombre de Films</div></div>
                <div class="kpi-card"><div class="kpi-value">{avg_rating_str}</div><div class="kpi-label">Note Moyenne</div></div>
                <div class="kpi-card"><div class="kpi-value">{revenue_str}</div><div class="kpi-label">Recettes Totales</div></div>
            </div>

            <div class="charts-grid">
                <div class="chart-wrapper">{div_genre}</div>
                <div class="chart-wrapper">{div_country}</div>
                <div class="chart-wrapper">{div_year}</div>
                <div class="chart-wrapper">{div_revenue}</div>
            </div>
        </div>

        <!-- CONTENU ONGLET 2 : CASTING EXPLORER (MODE CLAIR) -->
        <div id="casting-tab" class="tab-content">
            <div class="casting-container">
                <div class="casting-header">
                    <h2>🎭 Casting Network Explorer</h2>
                    <p>Fiches d'identité et indicateurs financiers des acteurs réels de CineScoop.</p>
                    
                    <!-- Raccourcis de Stars Populaires -->
                    <div class="filter-container" style="text-align: center; margin: 15px 0; display: flex; justify-content: center; gap: 20px; flex-wrap: wrap;">
                        <div>
                            <label for="nationality-filter" style="font-weight: 600; margin-right: 10px; color: #4b5563;">Filtre Nationalité :</label>
                            <select id="nationality-filter" onchange="filterStars()" style="padding: 8px 12px; border-radius: 8px; border: 1px solid #d1d5db; background-color: white; outline: none; cursor: pointer; font-family: 'Inter', sans-serif; font-size: 0.9em; min-width: 200px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                                <option value="all">🌍 Toutes les nationalités</option>
                                <!-- Options générées en JS -->
                            </select>
                        </div>
                        <div>
                            <label for="awards-filter" style="font-weight: 600; margin-right: 10px; color: #4b5563;">Récompenses :</label>
                            <select id="awards-filter" onchange="filterStars()" style="padding: 8px 12px; border-radius: 8px; border: 1px solid #d1d5db; background-color: white; outline: none; cursor: pointer; font-family: 'Inter', sans-serif; font-size: 0.9em; min-width: 200px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                                <option value="all">🏆 Tous niveaux confondus</option>
                                <option value="high">🌟 Superstars (&gt; 20 awards)</option>
                                <option value="medium">⭐ Stars confirmées (10 - 20 awards)</option>
                                <option value="low">✨ Talents montants (&lt; 10 awards)</option>
                            </select>
                        </div>
                    </div>

                    <div class="top-stars-row" id="top-stars-row">
                        <!-- Généré en JS -->
                    </div>

                    <div class="selector-box">
                        <!-- Moteur de recherche autocomplete -->
                        <div class="search-wrapper">
                            <input type="text" id="actor-search" class="search-input" placeholder="Rechercher une star..." oninput="filterSuggestions(this.value)">
                            <div id="search-suggestions" class="search-suggestions"></div>
                        </div>

                        <!-- Dropdown de secours -->
                        <select id="actor-select" onchange="selectActor(this.value)">
                            <!-- Chargé en JS -->
                        </select>
                    </div>
                </div>

                <!-- Profil Interactif de l'Acteur -->
                <div id="actor-profile-card" class="actor-profile" style="display: none;">
                    <div class="actor-hero">
                        <div class="actor-avatar" id="actor-avatar">--</div>
                        <div class="actor-details">
                            <h3 id="actor-name">Nom complet</h3>
                            <div class="actor-badges">
                                <span class="badge" id="actor-nationality">🌍 Nationalité</span>
                                <span class="badge badge-gold" id="actor-awards">🏆 0 Récompenses</span>
                            </div>
                        </div>
                    </div>

                    <!-- Grille Métriques Star -->
                    <div class="actor-stats-grid">
                        <div class="actor-stat-card">
                            <div class="actor-stat-val" id="stat-screen-time">0 min</div>
                            <div class="actor-stat-lbl">Temps d'écran cumulé</div>
                        </div>
                        <div class="actor-stat-card">
                            <div class="actor-stat-val" id="stat-movies-count">0 film</div>
                            <div class="actor-stat-lbl">Nombre de rôles</div>
                        </div>
                        <div class="actor-stat-card">
                            <div class="actor-stat-val" id="stat-avg-budget">0 M$</div>
                            <div class="actor-stat-lbl">Budget Moyen des films</div>
                        </div>
                        <div class="actor-stat-card">
                            <div class="actor-stat-val" id="stat-avg-revenue">0 M$</div>
                            <div class="actor-stat-lbl">Box-office Moyen</div>
                        </div>
                    </div>

                    <!-- Filmographie de la star -->
                    <h4 class="section-title">🎬 Filmographie & Rôles clés</h4>
                    <div class="filmography-grid" id="filmography-grid">
                        <!-- Rempli en JS avec effet stagger -->
                    </div>
                </div>

                <!-- État vide initial -->
                <div id="no-actor-selected" class="empty-state">
                    <div class="empty-icon">🎬</div>
                    <h3>Recherchez ou sélectionnez une star ci-dessus</h3>
                    <p>Cliquez sur l'une des bulles de stars ou tapez son nom pour voir la magie opérer !</p>
                </div>
            </div>
        </div>

        <!-- SCRIPTS JS -->
        <script>
            const actorData = {actor_json};
            const topStars = {top_actors_json};

            // Switch entre onglets
            function switchTab(tabId) {{
                document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

                if (tabId === 'dashboard') {{
                    document.querySelector('.tab-btn[onclick="switchTab(\\'dashboard\\')"]').classList.add('active');
                    document.getElementById('dashboard-tab').classList.add('active');
                }} else {{
                    document.querySelector('.tab-btn[onclick="switchTab(\\'casting\\')"]').classList.add('active');
                    document.getElementById('casting-tab').classList.add('active');
                }}
            }}

            // Remplissage du select dropdown
            const selectElement = document.getElementById('actor-select');
            const defaultOpt = document.createElement('option');
            defaultOpt.value = "";
            defaultOpt.text = "-- Choisir un Acteur --";
            selectElement.appendChild(defaultOpt);

            const sortedActorIds = Object.keys(actorData).sort((a, b) => {{
                return actorData[a].name.localeCompare(actorData[b].name);
            }});

            sortedActorIds.forEach(id => {{
                const actor = actorData[id];
                const opt = document.createElement('option');
                opt.value = id;
                opt.text = `${{actor.name}}`;
                selectElement.appendChild(opt);
            }});

            // Générer les options de nationalité dynamiquement
            const nationalitySelect = document.getElementById('nationality-filter');
            const uniqueNationalities = [...new Set(Object.values(actorData).map(a => a.nationality))].filter(n => n).sort();
            uniqueNationalities.forEach(nat => {{
                const opt = document.createElement('option');
                opt.value = nat;
                opt.text = nat;
                nationalitySelect.appendChild(opt);
            }});

            // Fonction pour afficher les bulles de stars
            function renderTopStars(starsToRender) {{
                const topStarsRow = document.getElementById('top-stars-row');
                topStarsRow.innerHTML = '';
                
                if (starsToRender.length === 0) {{
                    topStarsRow.innerHTML = '<div style="color: #6b7280; font-size: 0.9em; padding: 10px; text-align: center; width: 100%;">Aucune star trouvée pour cette nationalité.</div>';
                    return;
                }}

                starsToRender.forEach(star => {{
                    const bubble = document.createElement('div');
                    bubble.className = 'star-bubble';
                    const initials = star.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
                    // On gère l'image en récupérant depuis actorData si elle n'est pas dans topStars
                    const actorInfo = Object.values(actorData).find(a => a.id === star.id) || star;
                    const imageUrl = actorInfo.image_url || star.image_url;
                    
                    const avatarContent = imageUrl ? `<img src="${{imageUrl}}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.style.display='none'; this.parentElement.innerText='${{initials}}';" />` : initials;
                    bubble.innerHTML = `
                        <div class="bubble-avatar">${{avatarContent}}</div>
                        <div>
                            <div style="font-weight: 700; font-size: 0.95em;">${{star.name}}</div>
                            <div style="font-size: 0.75em; color: #6b7280;">🔥 ${{star.movies_count || star.movie_count}} films</div>
                        </div>
                    `;
                    bubble.onclick = () => {{
                        document.getElementById('actor-select').value = star.id;
                        document.getElementById('actor-search').value = star.name;
                        loadActorProfile(star.id);
                    }};
                    topStarsRow.appendChild(bubble);
                }});
            }}

            // Fonction de filtrage appelée par le dropdown
            window.filterStars = function() {{
                const selectedNat = document.getElementById('nationality-filter').value;
                const selectedAwards = document.getElementById('awards-filter').value;

                if (selectedNat === 'all' && selectedAwards === 'all') {{
                    renderTopStars(topStars);
                }} else {{
                    const filteredStars = Object.values(actorData).filter(a => {{
                        let matchNat = (selectedNat === 'all') || (a.nationality === selectedNat);
                        let matchAwards = true;
                        
                        if (selectedAwards === 'high') matchAwards = a.awards > 20;
                        else if (selectedAwards === 'medium') matchAwards = (a.awards >= 10 && a.awards <= 20);
                        else if (selectedAwards === 'low') matchAwards = a.awards < 10;
                        
                        return matchNat && matchAwards;
                    }}).sort((a, b) => b.total_screen_time - a.total_screen_time).slice(0, 10);
                        
                    const formattedFiltered = filteredStars.map(a => ({{
                        id: a.id,
                        name: a.name,
                        movies_count: a.movie_count,
                        image_url: a.image_url
                    }}));
                    
                    renderTopStars(formattedFiltered);
                }}
            }};

            // Initialisation
            renderTopStars(topStars);

            // Autocomplete Search logic
            function filterSuggestions(query) {{
                const suggestionsDiv = document.getElementById('search-suggestions');
                suggestionsDiv.innerHTML = '';
                if (!query) {{
                    suggestionsDiv.style.display = 'none';
                    return;
                }}

                const matchingIds = Object.keys(actorData).filter(id => {{
                    return actorData[id].name.toLowerCase().includes(query.toLowerCase());
                }});

                if (matchingIds.length === 0) {{
                    suggestionsDiv.style.display = 'none';
                    return;
                }}

                matchingIds.forEach(id => {{
                    const div = document.createElement('div');
                    div.className = 'suggestion-item';
                    div.innerText = actorData[id].name;
                    div.onclick = () => {{
                        document.getElementById('actor-search').value = actorData[id].name;
                        document.getElementById('actor-select').value = id;
                        loadActorProfile(id);
                        suggestionsDiv.style.display = 'none';
                    }};
                    suggestionsDiv.appendChild(div);
                }});
                suggestionsDiv.style.display = 'block';
            }}

            // Cacher les suggestions en cliquant à côté
            document.addEventListener('click', function(e) {{
                if (e.target.id !== 'actor-search') {{
                    document.getElementById('search-suggestions').style.display = 'none';
                }}
            }});

            // Appel depuis le dropdown de secours
            function selectActor(id) {{
                if (id) {{
                    document.getElementById('actor-search').value = actorData[id].name;
                    loadActorProfile(id);
                }} else {{
                    document.getElementById('actor-search').value = '';
                    loadActorProfile('');
                }}
            }}

            // Chargement du profil avec animations stagger (cascade)
            function loadActorProfile(actorId) {{
                const profileCard = document.getElementById('actor-profile-card');
                const emptyState = document.getElementById('no-actor-selected');

                if (!actorId) {{
                    profileCard.style.display = 'none';
                    emptyState.style.display = 'block';
                    return;
                }}

                const actor = actorData[actorId];

                // Données de base
                document.getElementById('actor-name').innerText = actor.name;
                document.getElementById('actor-nationality').innerText = `🌍 ${{actor.nationality}}`;
                document.getElementById('actor-awards').innerText = `🏆 ${{actor.awards}} Récompenses`;

                // Statistiques globales
                document.getElementById('stat-screen-time').innerText = `${{actor.total_screen_time}} min`;
                document.getElementById('stat-movies-count').innerText = `${{actor.movie_count}} film${{actor.movie_count > 1 ? 's' : ''}}`;
                document.getElementById('stat-avg-budget').innerText = `${{(actor.avg_budget / 1e6).toFixed(1)}} M$`;
                document.getElementById('stat-avg-revenue').innerText = `${{(actor.avg_revenue / 1e6).toFixed(1)}} M$`;

                // Avatar (Photo ou Initiales)
                const avatar = document.getElementById('actor-avatar');
                const initials = actor.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
                if (actor.image_url) {{
                    avatar.innerHTML = `<img src="${{actor.image_url}}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.style.display='none'; this.parentElement.innerText='${{initials}}';" />`;
                }} else {{
                    avatar.innerText = initials;
                }}

                // Grille de films avec effet d'entrée cascade (stagger)
                const grid = document.getElementById('filmography-grid');
                grid.innerHTML = '';

                actor.movies.forEach((movie, index) => {{
                    const card = document.createElement('div');
                    card.className = 'film-card';
                    card.style.opacity = 0;
                    card.style.transform = 'translateY(20px)';
                    card.style.transition = 'all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';

                    const budgetText = movie.budget > 0 ? `${{(movie.budget / 1e6).toFixed(1)}} M$` : "N/A";
                    const revenueText = movie.revenue > 0 ? `${{(movie.revenue / 1e6).toFixed(1)}} M$` : "N/A";

                    card.innerHTML = `
                        <div class="film-header">
                            <h5>${{movie.title}}</h5>
                            <span class="film-year">${{movie.year}}</span>
                        </div>
                        <div class="film-row"><strong>Rôle incarné :</strong> <span class="role-badge">${{movie.role}}</span></div>
                        <div class="film-row"><strong>Temps d'écran :</strong> ${{movie.screen_time}} minutes</div>
                        <div class="film-financials">
                            <span class="fin-badge fin-budget">💰 Budget : ${{budgetText}}</span>
                            <span class="fin-badge fin-revenue">🍿 Box-Office : ${{revenueText}}</span>
                        </div>
                    `;
                    grid.appendChild(card);

                    // Lancement décalé de l'animation de chaque carte
                    setTimeout(() => {{
                        card.style.opacity = 1;
                        card.style.transform = 'translateY(0)';
                    }}, index * 100);
                }});

                emptyState.style.display = 'none';
                profileCard.style.display = 'block';
            }}
        </script>
    </body>
    </html>
    """

    output_file = 'dashboard/dashboard_cinescoop.html'
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"[OK] Dashboard genere avec succes ! Ouvrez le fichier '{os.path.abspath(output_file)}' dans votre navigateur.")

if __name__ == "__main__":
    generate_dashboard()
