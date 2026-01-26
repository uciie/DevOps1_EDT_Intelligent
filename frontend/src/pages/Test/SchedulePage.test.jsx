import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SchedulePage from '../../pages/SchedulePage';
import * as authApi from '../../api/authApi';
import * as taskApi from '../../api/taskApi';
import * as eventApi from '../../api/eventApi';
import * as teamApi from '../../api/teamApi';
import * as userApi from '../../api/userApi';

// --- MOCK DES COMPOSANTS ENFANTS ---

vi.mock('../../components/Calendar', () => ({
  default: ({ onAddEventRequest, contextTeam }) => (
    <div data-testid="mock-calendar">
      <span>Calendar for: {contextTeam ? contextTeam.name : 'Personal'}</span>
      <button 
        data-testid="btn-cell-click" 
        onClick={() => onAddEventRequest(new Date('2024-01-01'), 10)}
      >
        Click Cell
      </button>
    </div>
  )
}));

vi.mock('../../components/TodoList', () => ({
  default: ({ tasks, onAddTask }) => (
    <div data-testid="mock-todolist">
      <span>Task Count: {tasks.length}</span>
      <button 
        data-testid="btn-add-task" 
        onClick={() => onAddTask({ title: 'Nouvelle Tâche', priority: 2 })}
      >
        Add Task
      </button>
    </div>
  )
}));

vi.mock('../../components/form/EventForm', () => ({
  default: ({ isOpen, onSave, onClose }) => {
    if (!isOpen) return null;
    return (
      <div data-testid="mock-event-form">
        <h2>Formulaire Ouvert</h2>
        <button onClick={() => onSave({ summary: 'Mon Event Test', color: '#000' })}>
          Sauvegarder
        </button>
        <button onClick={onClose}>Fermer</button>
      </div>
    );
  }
}));

// AJOUT : Mock de Notification pour éviter les animations et garantir l'affichage du texte
vi.mock('../../components/Notification', () => ({
  default: ({ message }) => (
    message ? <div data-testid="mock-notification">{message}</div> : null
  )
}));

vi.mock('../../components/ChatAssistant', () => ({
  default: () => <div data-testid="mock-chat">Chat</div>
}));

// --- MOCK DES APIS ---
vi.mock('../../api/authApi');
vi.mock('../../api/taskApi');
vi.mock('../../api/eventApi');
vi.mock('../../api/teamApi');
vi.mock('../../api/userApi');

describe('SchedulePage', () => {
  const mockUser = { id: 1, username: 'TestUser' };
  
  const initialTasks = [
    { id: 101, title: 'Task 1', userId: 1 },
    { id: 102, title: 'Task 2', userId: 1 }
  ];
  const initialEvents = [
    { id: 201, summary: 'Event 1', startTime: '2024-01-01T10:00:00', userId: 1 }
  ];
  const initialTeams = [
    { id: 5, name: 'DevTeam', ownerId: 1, members: [] }
  ];

  beforeEach(() => {
    vi.clearAllMocks();

    authApi.getCurrentUser.mockReturnValue(mockUser);
    
    taskApi.getUserTasks.mockResolvedValue(initialTasks);
    taskApi.getDelegatedTasks.mockResolvedValue([]);
    
    eventApi.getUserEvents.mockResolvedValue(initialEvents);
    
    teamApi.getMyTeams.mockResolvedValue(initialTeams);
  });

  it('charge et affiche les données correctement au démarrage', async () => {
    render(<SchedulePage />);
    expect(screen.getByText(/Chargement.../i)).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/Bonjour, TestUser/i)).toBeInTheDocument();
    });
    expect(screen.getByTestId('mock-todolist')).toHaveTextContent('Task Count: 2');
    expect(screen.getByTestId('mock-calendar')).toHaveTextContent('Calendar for: Personal');
    expect(screen.getByText('DevTeam')).toBeInTheDocument();
  });

  it('permet de basculer vers une vue d\'équipe', async () => {
    render(<SchedulePage />);
    await waitFor(() => screen.getByText(/Bonjour, TestUser/i));

    expect(screen.getByText('Votre espace personnel')).toBeInTheDocument();

    const teamItem = screen.getByText('DevTeam');
    fireEvent.click(teamItem);

    expect(screen.getByText('Espace de travail : DevTeam')).toBeInTheDocument();
    expect(screen.getByTestId('mock-calendar')).toHaveTextContent('Calendar for: DevTeam');
  });

  it('ouvre le formulaire de création d\'événement lors d\'un clic sur le calendrier', async () => {
    render(<SchedulePage />);
    await waitFor(() => screen.getByText(/Bonjour, TestUser/i));

    expect(screen.queryByTestId('mock-event-form')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('btn-cell-click'));
    expect(screen.getByTestId('mock-event-form')).toBeInTheDocument();
  });

  it('gère l\'ajout d\'une tâche via la TodoList', async () => {
    const newTask = { id: 103, title: 'Nouvelle Tâche', userId: 1 };
    taskApi.createTask.mockResolvedValue(newTask);

    render(<SchedulePage />);
    await waitFor(() => screen.getByText(/Bonjour, TestUser/i));

    fireEvent.click(screen.getByTestId('btn-add-task'));

    await waitFor(() => {
      expect(taskApi.createTask).toHaveBeenCalledWith(expect.objectContaining({
        title: 'Nouvelle Tâche',
        userId: 1
      }));
      expect(screen.getByText(/Tâche ajoutée avec succès/i)).toBeInTheDocument();
    });

    expect(screen.getByTestId('mock-todolist')).toHaveTextContent('Task Count: 3');
  });

  it('gère la sauvegarde d\'un événement', async () => {
    // CORRECTION ICI : Une date valide (ISO string) est nécessaire pour éviter le crash
    eventApi.createEvent.mockResolvedValue({ 
      id: 202, 
      summary: 'Mon Event Test', 
      startTime: '2024-01-01T12:00:00' // Date valide !
    });

    render(<SchedulePage />);
    await waitFor(() => screen.getByText(/Bonjour, TestUser/i));

    // 1. Ouvrir le formulaire
    fireEvent.click(screen.getByTestId('btn-cell-click'));
    
    // 2. Sauvegarder
    fireEvent.click(screen.getByText('Sauvegarder'));

    // 3. Vérification
    await waitFor(() => {
      expect(eventApi.createEvent).toHaveBeenCalled();
      // Le texte doit maintenant apparaître grâce à la date valide
      expect(screen.getByText(/Événement créé avec succès/i)).toBeInTheDocument();
    });
    
    expect(screen.queryByTestId('mock-event-form')).not.toBeInTheDocument();
  });

  it('appelle la réorganisation (reshuffle) au clic sur le bouton', async () => {
    taskApi.reshuffleSchedule.mockResolvedValue({});
    taskApi.getUserTasks.mockResolvedValue([...initialTasks]); 

    render(<SchedulePage />);
    await waitFor(() => screen.getByText(/Bonjour, TestUser/i));

    const reshuffleBtn = screen.getByText(/Réorganiser l'agenda/i);
    fireEvent.click(reshuffleBtn);

    await waitFor(() => {
      expect(taskApi.reshuffleSchedule).toHaveBeenCalledWith(1);
      expect(screen.getByText(/Agenda réorganisé avec succès/i)).toBeInTheDocument();
    });
  });

  it('affiche une erreur si l\'utilisateur n\'est pas connecté', async () => {
    authApi.getCurrentUser.mockReturnValue(null);

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText(/Oups !/i)).toBeInTheDocument();
      expect(screen.getByText(/Utilisateur non connecté/i)).toBeInTheDocument();
    });
  });
});