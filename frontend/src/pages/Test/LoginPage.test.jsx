import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
// Attention aux chemins : on remonte d'un cran pour les pages, et de deux pour l'api
import LoginPage from '../LoginPage';
import { loginUser } from '../../api/authApi';

// 1. Mock explicite : on force loginUser à être une fonction simulée (spy)
vi.mock('../../api/authApi', () => ({
  loginUser: vi.fn(),
}));

// 2. Mock de useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche le formulaire de connexion correctement', () => {
    render(
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    );
    
    expect(screen.getByLabelText(/Nom d'utilisateur/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Mot de passe/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Se connecter/i })).toBeInTheDocument();
  });

  it('gère une connexion réussie', async () => {
    // Configuration de la réponse simulée
    const mockUser = { username: 'testuser', token: 'fake-token' };
    loginUser.mockResolvedValue(mockUser);
    
    const mockOnLogin = vi.fn();

    render(
      <BrowserRouter>
        <LoginPage onLogin={mockOnLogin} />
      </BrowserRouter>
    );

    // Interaction utilisateur
    fireEvent.change(screen.getByLabelText(/Nom d'utilisateur/i), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByLabelText(/Mot de passe/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /Se connecter/i }));

    await waitFor(() => {
      // Vérifications
      expect(loginUser).toHaveBeenCalledWith({ username: 'testuser', password: 'password123' });
      expect(mockOnLogin).toHaveBeenCalledWith(mockUser);
      expect(mockNavigate).toHaveBeenCalledWith('/schedule', { replace: true });
    });
  });

  it('affiche une erreur si les identifiants sont incorrects', async () => {
    // Simulation d'une erreur
    loginUser.mockRejectedValue(new Error('Invalid credentials'));

    render(
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    );

    fireEvent.change(screen.getByLabelText(/Nom d'utilisateur/i), { target: { value: 'wrong' } });
    fireEvent.change(screen.getByLabelText(/Mot de passe/i), { target: { value: 'wrong' } });
    fireEvent.click(screen.getByRole('button', { name: /Se connecter/i }));

    await waitFor(() => {
      expect(screen.getByText(/Nom d'utilisateur ou mot de passe incorrect/i)).toBeInTheDocument();
    });
  });
});