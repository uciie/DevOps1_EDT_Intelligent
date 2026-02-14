import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import SetupPage from '../../pages/SetupPage';
import * as authApi from '../../api/authApi';
import * as userApi from '../../api/userApi';
import * as eventApi from '../../api/eventApi';
import api from '../../api/api';

// Mock des APIs
vi.mock('../../api/authApi');
vi.mock('../../api/userApi');
vi.mock('../../api/eventApi');
vi.mock('../../api/api');

// Mock de useLocation pour tester les notifications de succès
const mockLocation = { state: null, pathname: '/setup', search: '', hash: '' };
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useLocation: () => mockLocation,
    Link: ({ children, to }) => <a href={to}>{children}</a>
  };
});

describe('SetupPage', () => {
  const mockUser = {
    id: 1,
    username: 'testuser',
    googleRefreshToken: null
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    mockLocation.state = null;
    
    // Setup par défaut
    authApi.getCurrentUser.mockReturnValue(mockUser);
    userApi.getGoogleAuthUrl.mockReturnValue('https://accounts.google.com/oauth2');
    api.get.mockResolvedValue({ data: mockUser });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS DE RENDU INITIAL
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Rendu initial', () => {
    it('✅ Devrait afficher le titre de la page', () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      expect(screen.getByText(/Configuration/i)).toBeInTheDocument();
      expect(screen.getByText(/Personnalisez le comportement de l'application/i)).toBeInTheDocument();
    });

    it('✅ Devrait afficher le toggle Google Maps', () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      expect(screen.getByText(/Mode de calcul des trajets/i)).toBeInTheDocument();
      const toggle = document.querySelector('.switch input[type="checkbox"]');
      expect(toggle).toBeInTheDocument();
    });

    it('✅ Devrait afficher la section Google Calendar', () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      expect(screen.getByText(/Intégrations/i)).toBeInTheDocument();
      expect(screen.getByText(/Google Calendar/i)).toBeInTheDocument();
    });

    it('✅ Devrait charger la préférence Google Maps depuis localStorage', () => {
      // Given
      localStorage.setItem('useGoogleMaps', 'false');

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      const toggle = document.querySelector('.switch input[type="checkbox"]');
      expect(toggle).not.toBeChecked();
    });

    it('✅ Devrait être coché par défaut si pas de préférence', () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      const toggle = document.querySelector('.switch input[type="checkbox"]');
      expect(toggle).toBeChecked();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS DE L'AUTHENTIFICATION GOOGLE
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Authentification Google', () => {
    it('✅ Devrait afficher "Déconnecté" si pas de token', async () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Déconnecté/i)).toBeInTheDocument();
      });
    });

    it('✅ Devrait afficher "Connecté" si token présent', async () => {
      // Given
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Connecté/i)).toBeInTheDocument();
      });
    });

    it('✅ Devrait afficher le bouton "Se connecter avec Google"', async () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        const btn = screen.getByRole('button', { name: /Se connecter avec Google/i });
        expect(btn).toBeInTheDocument();
      });
    });

    it('✅ Devrait rediriger vers Google OAuth au clic', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      delete window.location;
      window.location = { href: '' };

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Se connecter avec Google/i })).toBeInTheDocument();
      });

      const googleBtn = screen.getByRole('button', { name: /Se connecter avec Google/i });
      await user.click(googleBtn);

      // Then
      expect(window.location.href).toBe('https://accounts.google.com/oauth2');
    });

    it('✅ Devrait afficher le bouton "Déconnecter" si connecté', async () => {
      // Given
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Déconnecter/i })).toBeInTheDocument();
      });
    });

    it('✅ Devrait appeler unlinkGoogleAccount au clic sur déconnecter', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });
      userApi.unlinkGoogleAccount.mockResolvedValue({});
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Déconnecter/i })).toBeInTheDocument();
      });

      const unlinkBtn = screen.getByRole('button', { name: /Déconnecter/i });
      await user.click(unlinkBtn);

      // Then
      await waitFor(() => {
        expect(userApi.unlinkGoogleAccount).toHaveBeenCalledWith(mockUser.id);
      });

      confirmSpy.mockRestore();
    });

    it('⚠️ Ne devrait PAS déconnecter si l\'utilisateur annule', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Déconnecter/i })).toBeInTheDocument();
      });

      const unlinkBtn = screen.getByRole('button', { name: /Déconnecter/i });
      await user.click(unlinkBtn);

      // Then
      expect(userApi.unlinkGoogleAccount).not.toHaveBeenCalled();

      confirmSpy.mockRestore();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS DU TOGGLE GOOGLE MAPS
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Toggle Google Maps', () => {
    it('✅ Devrait mettre à jour localStorage au changement', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockResolvedValue({});

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      expect(localStorage.getItem('useGoogleMaps')).toBe('false');
    });

    it('✅ Devrait appeler recalculateTravelTimes au changement', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockResolvedValue({});

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      await waitFor(() => {
        expect(eventApi.recalculateTravelTimes).toHaveBeenCalledWith(mockUser.id, false);
      });
    });

    it('✅ Devrait afficher un message de chargement pendant recalcul', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      expect(screen.getByText(/Recalcul des trajets en cours/i)).toBeInTheDocument();
    });

    it('✅ Devrait rediriger vers /schedule après succès', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockResolvedValue({});
      delete window.location;
      window.location = { href: '' };

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Mise à jour terminée/i)).toBeInTheDocument();
      }, { timeout: 3000 });

      // La redirection se fait après 1 seconde
      vi.advanceTimersByTime(1000);
      
      expect(window.location.href).toBe('/schedule');
    });

    it('⚠️ Devrait gérer les erreurs de recalcul', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockRejectedValue(new Error('Recalcul failed'));

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Erreur lors de la mise à jour/i)).toBeInTheDocument();
      });
    });

    it('✅ Devrait désactiver le toggle pendant le chargement', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      expect(toggle).toBeDisabled();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS DES NOTIFICATIONS
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Notifications de succès', () => {
    it('✅ Devrait afficher la notification si vient de GoogleCallback', () => {
      // Given
      mockLocation.state = { 
        success: true, 
        message: 'Compte Google lié avec succès !' 
      };

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      expect(screen.getByText(/Compte Google lié avec succès/i)).toBeInTheDocument();
    });

    it('✅ Devrait afficher la notification de déconnexion', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });
      userApi.unlinkGoogleAccount.mockResolvedValue({});
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Déconnecter/i })).toBeInTheDocument();
      });

      const unlinkBtn = screen.getByRole('button', { name: /Déconnecter/i });
      await user.click(unlinkBtn);

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Compte Google déconnecté avec succès/i)).toBeInTheDocument();
      });

      confirmSpy.mockRestore();
    });

    it('✅ Devrait fermer la notification au clic', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      mockLocation.state = { 
        success: true, 
        message: 'Test notification' 
      };

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      expect(screen.getByText(/Test notification/i)).toBeInTheDocument();

      // Trouver et cliquer sur le bouton de fermeture
      const closeButtons = screen.getAllByRole('button');
      const closeBtn = closeButtons.find(btn => btn.textContent === '×');
      
      if (closeBtn) {
        await user.click(closeBtn);
      }

      // Then
      await waitFor(() => {
        expect(screen.queryByText(/Test notification/i)).not.toBeInTheDocument();
      });
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS D'INTÉGRATION
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Intégration complète', () => {
    it('✅ Devrait charger les données utilisateur au montage', async () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        expect(api.get).toHaveBeenCalledWith(`/users/username/${mockUser.username}`);
      });
    });

    it('✅ Devrait détecter le statut Google à partir du token', async () => {
      // Given
      const connectedUser = { ...mockUser, googleRefreshToken: 'valid-token' };
      api.get.mockResolvedValue({ data: connectedUser });

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      await waitFor(() => {
        expect(screen.getByText(/Connecté/i)).toBeInTheDocument();
      });
    });

    it('✅ Devrait permettre le flux complet : connexion → déconnexion', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      const disconnectedUser = { ...mockUser, googleRefreshToken: null };
      const connectedUser = { ...mockUser, googleRefreshToken: 'new-token' };
      
      api.get.mockResolvedValueOnce({ data: disconnectedUser });
      delete window.location;
      window.location = { href: '' };

      // When - État initial déconnecté
      const { rerender } = render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Déconnecté/i)).toBeInTheDocument();
      });

      // Simulation de reconnexion après OAuth
      api.get.mockResolvedValueOnce({ data: connectedUser });
      
      rerender(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Connecté/i)).toBeInTheDocument();
      });

      // Déconnexion
      const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
      userApi.unlinkGoogleAccount.mockResolvedValue({});

      const unlinkBtn = screen.getByRole('button', { name: /Déconnecter/i });
      await user.click(unlinkBtn);

      await waitFor(() => {
        expect(userApi.unlinkGoogleAccount).toHaveBeenCalledWith(mockUser.id);
      });

      confirmSpy.mockRestore();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS DE CAS LIMITES
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Cas limites', () => {
    it('⚠️ Devrait gérer l\'absence d\'utilisateur', () => {
      // Given
      authApi.getCurrentUser.mockReturnValue(null);

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then - Devrait afficher la page sans crash
      expect(screen.getByText(/Configuration/i)).toBeInTheDocument();
    });

    it('⚠️ Devrait gérer les erreurs de chargement utilisateur', async () => {
      // Given
      api.get.mockRejectedValue(new Error('User load failed'));

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then - Pas de crash
      expect(screen.getByText(/Configuration/i)).toBeInTheDocument();
    });

    it('⚠️ Devrait gérer une valeur invalide dans localStorage', () => {
      // Given
      localStorage.setItem('useGoogleMaps', 'invalid-json');

      // When - Ne devrait pas crasher
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      const toggle = document.querySelector('.switch input[type="checkbox"]');
      expect(toggle).toBeInTheDocument();
    });
  });

  // ═══════════════════════════════════════════════════════════════════════════
  // TESTS D'ACCESSIBILITÉ
  // ═══════════════════════════════════════════════════════════════════════════

  describe('Accessibilité', () => {
    it('✅ Devrait avoir un lien retour vers l\'emploi du temps', () => {
      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      // Then
      const backLink = screen.getByRole('link', { name: /Retour à l'emploi du temps/i });
      expect(backLink).toBeInTheDocument();
      expect(backLink).toHaveAttribute('href', '/schedule');
    });

    it('✅ Devrait désactiver le lien retour pendant le chargement', async () => {
      // Given
      const user = userEvent.setup({ delay: null });
      eventApi.recalculateTravelTimes.mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      // When
      render(
        <BrowserRouter>
          <SetupPage />
        </BrowserRouter>
      );

      const toggle = document.querySelector('.switch input[type="checkbox"]');
      await user.click(toggle);

      // Then
      const backLink = screen.getByRole('link', { name: /Retour à l'emploi du temps/i });
      expect(backLink).toHaveClass('disabled');
    });
  });
});