import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import RegisterPage from '../RegisterPage';
import { registerUser } from '../../api/userApi';

// Mock explicite pour registerUser
vi.mock('../../api/userApi', () => ({
  registerUser: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche une erreur si les mots de passe ne correspondent pas', async () => {
    render(
      <BrowserRouter>
        <RegisterPage />
      </BrowserRouter>
    );

    fireEvent.change(screen.getByLabelText(/^Nom d'utilisateur/i), { target: { value: 'newuser' } });
    fireEvent.change(screen.getByLabelText(/^Mot de passe/i), { target: { value: '1234' } });
    fireEvent.change(screen.getByLabelText(/Confirmer le mot de passe/i), { target: { value: '5678' } });

    fireEvent.click(screen.getByRole('button', { name: /Créer mon compte/i }));

    expect(screen.getByText(/Les mots de passe ne correspondent pas/i)).toBeInTheDocument();
    // Ici, on vérifie bien sur le mock importé directement
    expect(registerUser).not.toHaveBeenCalled();
  });

  it('gère une inscription réussie', async () => {
    registerUser.mockResolvedValue(123);

    render(
      <BrowserRouter>
        <RegisterPage />
      </BrowserRouter>
    );

    fireEvent.change(screen.getByLabelText(/^Nom d'utilisateur/i), { target: { value: 'newuser' } });
    fireEvent.change(screen.getByLabelText(/^Mot de passe/i), { target: { value: 'secret' } });
    fireEvent.change(screen.getByLabelText(/Confirmer le mot de passe/i), { target: { value: 'secret' } });

    fireEvent.click(screen.getByRole('button', { name: /Créer mon compte/i }));

    await waitFor(() => {
      expect(registerUser).toHaveBeenCalledWith({ username: 'newuser', password: 'secret' });
      expect(mockNavigate).toHaveBeenCalledWith('/schedule', { replace: true });
    });
  });
});