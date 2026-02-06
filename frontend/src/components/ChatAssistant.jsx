import React, { useState, useEffect, useRef } from "react";
import "../styles/components/ChatAssistant.css";
import { getCurrentUser } from "../api/authApi";
import { sendChatMessage } from "../api/chatbotApi";

export default function ChatAssistant({onRefresh}) {
  const [isOpen, setIsOpen] = useState(true);
  const messagesEndRef = useRef(null);
  const now = new Date();
  const hh0 = String(now.getHours()).padStart(2, "0");
  const mm0 = String(now.getMinutes()).padStart(2, "0");
  const initialTime = `${hh0}:${mm0}`;

  const [messages, setMessages] = useState([
    {
      id: 1,
      author: "assistant",
      text: "Bonjour ! Je suis votre assistant de planification.\nComment puis-je vous aider aujourd'hui ?",
      time: initialTime,
    },
  ]);
  
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  
  const currentUser = getCurrentUser();

  // Détection de la clé API (pour afficher le statut)
  const apiKey = import.meta.env.VITE_CHATBOT_API_KEY || null;
  const isOnline = Boolean(apiKey);

  const quickActions = [
    "Quoi de prévu aujourd'hui ?",
    "Libère ma soirée",
    "Déplace mon sport à demain",
    "Ajoute 1h de lecture ce soir",
  ];

  // Fonction pour ajouter un message au chat
  function addMessage(text, author = "user") {
    if (!text || !text.trim()) return;
    const t = new Date();
    const hh = String(t.getHours()).padStart(2, "0");
    const mm = String(t.getMinutes()).padStart(2, "0");
    
    const newMessage = { 
      id: Date.now(), 
      author, 
      text, 
      time: `${hh}:${mm}` 
    };
    
    setMessages((m) => [...m, newMessage]);
    return newMessage;
  }

  // Fonction pour envoyer un message utilisateur
  async function sendMessage(text) {
    if (!text || !text.trim()) return;
    if (!currentUser) {
      addMessage("Vous devez être connecté pour utiliser l'assistant.", "assistant");
      return;
    }

    // 1. Afficher le message de l'utilisateur
    addMessage(text, "user");
    setInput("");
    setIsLoading(true);

    try {
      // 2. Appel API au backend
      const response = await sendChatMessage(text, currentUser.id);
      
      // 3. Afficher la réponse de l'assistant
      addMessage(response, "assistant");

      if (onRefresh) {
        onRefresh();
      }
      
    } catch (error) {
      console.error("Erreur chatbot:", error);
      addMessage("Désolé, une erreur s'est produite. Réessayez plus tard.", "assistant");
    } finally {
      setIsLoading(false);
    }
  }

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  return (
    <div className="chat-container">
      {!isOpen && (
        <button
          className="chat-toggle"
          aria-label="Ouvrir le chat"
          onClick={() => setIsOpen(true)}
        >
          <span className="chat-toggle-icon">💬</span>
        </button>
      )}

      {isOpen && (
        <div className="chat-widget">
          <div className="chat-header">
            <div className="chat-title">
              <div className="avatar">🤖</div>
              <div className="title-text">
                <div className="name">Assistant Planificateur</div>
                <div className="status">
                  <span className={`status-dot ${isOnline ? 'online' : 'offline'}`}></span>
                  <span className="status-text">{isOnline ? 'En ligne' : 'Hors ligne'}</span>
                </div>
              </div>
            </div>
            <button className="close" onClick={() => setIsOpen(false)}>✕</button>
          </div>

          <div className="chat-body">
            <div className="messages">
              {messages.map((m) => (
                <div
                  key={m.id}
                  className={`message-row ${m.author === "assistant" ? "assistant" : "user"}`}
                >
                  {m.author === "assistant" ? (
                    <>
                      <div className="msg-avatar">🤖</div>
                      <div className={`bubble assistant-bubble`}>
                        <div className="bubble-text">
                          {m.text.split('\n').map((line, i) => (<div key={i}>{line}</div>))}
                        </div>
                      </div>
                      <div className="bubble-time">{m.time}</div>
                    </>
                  ) : (
                    <>
                      <div className="bubble-time-left">{m.time}</div>
                      <div className={`bubble user-bubble`}>
                        <div className="bubble-text">
                          {m.text.split('\n').map((line, i) => (<div key={i}>{line}</div>))}
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ))}
              
              {/* Indicateur de chargement */}
              {isLoading && (
                <div className="message-row assistant">
                  <div className="msg-avatar">🤖</div>
                  <div className="bubble assistant-bubble">
                    <div className="typing-indicator">
                      <span></span><span></span><span></span>
                    </div>
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>

            <div className="quick-actions">
              {quickActions.map((q, i) => (
                <button
                  key={i}
                  className="quick-btn"
                  onClick={() => sendMessage(q)}
                  disabled={isLoading}
                >
                  {q}
                </button>
              ))}
            </div>
          </div>

          <div className="chat-input">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Tapez votre message..."
              onKeyDown={(e) => { if (e.key === 'Enter' && !isLoading) sendMessage(input); }}
              disabled={isLoading}
            />
            <button 
              className="send" 
              onClick={() => sendMessage(input)}
              disabled={isLoading || !input.trim()}
            >
              ➤
            </button>
          </div>
        </div>
      )}
    </div>
  );
}