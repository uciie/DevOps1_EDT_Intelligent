import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
// On remonte d'un niveau pour trouver la page
import ActivityPage from '../ActivityPage';
// On remonte de deux niveaux pour trouver les APIs
import * as activityApi from '../../api/activityApi';
import * as authApi from '../../api/authApi';

// Mock des dépendances
vi.mock('../../api/activityApi');
vi.mock('../../api/authApi');

describe('ActivityPage', () => {
  const mockUser = { id: 1, username: 'TestUser' };
  
  const mockStats = [
    { category: 'TRAVAIL', totalMinutes: 150, count: 3, averageMinutes: 50 },
    { category: 'SPORT', totalMinutes: 60, count: 1, averageMinutes: 60 },
    { category: 'LOISIR', totalMinutes: 30, count: 2, averageMinutes: 15 }
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    authApi.getCurrentUser.mockReturnValue(mockUser);
    activityApi.getActivityStats.mockResolvedValue(mockStats);
  });

  it('affiche le titre et charge les données initiales', async () => {
    render(<ActivityPage />);
    
    expect(screen.getByText(/Calcul des statistiques en cours/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(activityApi.getActivityStats).toHaveBeenCalled();
    });

    // On vérifie la présence des cartes via leur titre (h3)
    expect(screen.getByRole('heading', { level: 3, name: /Travail 💼/i })).toBeInTheDocument();
    expect(screen.getByText('2h 30m')).toBeInTheDocument();
    
    expect(screen.getByRole('heading', { level: 3, name: /Sport 🏃/i })).toBeInTheDocument();
  });

  it('filtre l’affichage lorsqu’on sélectionne une catégorie', async () => {
    render(<ActivityPage />);
    await waitFor(() => expect(screen.queryByText(/Calcul des statistiques/i)).not.toBeInTheDocument());

    // 1. Clic sur le bouton de filtre "Travail"
    const workFilterBtn = screen.getByRole('button', { name: /Travail 💼/i });
    fireEvent.click(workFilterBtn);
    
    expect(workFilterBtn).toHaveClass('selected');

    // 2. Clic sur "Confirmer"
    const searchBtn = screen.getByRole('button', { name: /Confirmer et Actualiser/i });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      // 3. Vérification : Seul "Travail" doit être affiché
      expect(screen.getByRole('heading', { level: 3, name: /Travail 💼/i })).toBeInTheDocument();
      
      // Les autres ne doivent pas être là
      expect(screen.queryByRole('heading', { level: 3, name: /Sport 🏃/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { level: 3, name: /Loisir 🎮/i })).not.toBeInTheDocument();
    });
  });

  it('change la période et recharge les données API', async () => {
    render(<ActivityPage />);
    await waitFor(() => expect(screen.queryByText(/Calcul des statistiques/i)).not.toBeInTheDocument());

    // Changement de période
    const monthBtn = screen.getByRole('button', { name: /30 jours/i });
    fireEvent.click(monthBtn);

    // Validation
    const searchBtn = screen.getByRole('button', { name: /Confirmer et Actualiser/i });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      // L'API doit avoir été appelée 2 fois (init + changement)
      expect(activityApi.getActivityStats).toHaveBeenCalledTimes(2);
    });
  });

  it('affiche un message si aucune donnée n’est trouvée', async () => {
    activityApi.getActivityStats.mockResolvedValue([]);

    render(<ActivityPage />);

    await waitFor(() => {
      expect(screen.getByText(/Aucune activité trouvée/i)).toBeInTheDocument();
    });
  });
}); 