import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
// On remonte d'un niveau pour trouver la page
import Home from '../Home';
// On remonte de deux niveaux pour trouver l'api
import * as authApi from '../../api/authApi';

// Mock de l'API d'authentification et du router
vi.mock('../../api/authApi');

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('Home Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche la vue invité quand aucun utilisateur n’est connecté', () => {
    // Simulation : pas d'utilisateur
    authApi.getCurrentUser.mockReturnValue(null);

    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    );

    // Vérifie le titre principal
    expect(screen.getByText(/Bienvenue sur EDT Intelligent/i)).toBeInTheDocument();
    
    // Vérifie la présence des boutons de connexion/inscription
    expect(screen.getByText(/Se connecter/i)).toBeInTheDocument();
    expect(screen.getByText(/Créer un compte/i)).toBeInTheDocument();
    
    // Vérifie que les liens pointent vers les bonnes pages
    expect(screen.getByRole('link', { name: /Se connecter/i })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: /Créer un compte/i })).toHaveAttribute('href', '/register');
    
    // Vérifie que la section utilisateur est masquée
    expect(screen.queryByText(/Bonjour,/i)).not.toBeInTheDocument();
  });

  it('affiche le tableau de bord quand un utilisateur est connecté', () => {
    // Simulation : utilisateur connecté
    const mockUser = { username: 'Thomas' };
    authApi.getCurrentUser.mockReturnValue(mockUser);

    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    );

    // Vérifie le message de bienvenue personnalisé
    expect(screen.getByText('Bonjour, Thomas !')).toBeInTheDocument();
    
    // Vérifie la présence de l'avatar (première lettre)
    expect(screen.getByText('T')).toBeInTheDocument();

    // Vérifie les boutons d'action
    expect(screen.getByText(/Voir mon emploi du temps/i)).toBeInTheDocument();
    expect(screen.getByText(/Mes Invitations/i)).toBeInTheDocument();
    expect(screen.getByText(/Se déconnecter/i)).toBeInTheDocument();
    
    // Les boutons "invité" ne doivent plus être là
    expect(screen.queryByText(/Créer un compte/i)).not.toBeInTheDocument();
  });

  it('gère la déconnexion correctement', () => {
    // Simulation : utilisateur connecté au départ
    authApi.getCurrentUser.mockReturnValue({ username: 'Thomas' });

    render(
      <BrowserRouter>
        <Home />
      </BrowserRouter>
    );

    // Clic sur le bouton de déconnexion
    const logoutBtn = screen.getByText(/Se déconnecter/i);
    fireEvent.click(logoutBtn);

    // Vérifie que logoutUser a été appelé
    expect(authApi.logoutUser).toHaveBeenCalledTimes(1);
    
    // Vérifie la redirection vers la racine "/"
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    
    // (Optionnel) Vérifier que l'interface revient à l'état invité
    // Note: Dans un test unitaire, le state ne change pas toujours aussi proprement 
    // sans wrapper complet, mais on peut vérifier l'appel des fonctions.
  });
});