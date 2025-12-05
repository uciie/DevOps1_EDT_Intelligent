import { useState, useEffect } from 'react';
import GooglePlacesAutocomplete from '../GooglePlacesAutocomplete';
import '../../styles/components/Event.css';

const CATEGORIES = [
  { id: 'TRAVAIL', label: 'Travail', color: '#3b82f6', subCategories: [] },
  { id: 'ETUDE', label: 'Études', color: '#8b5cf6', subCategories: ['COURS', 'TD', 'TP', 'REVISION', 'EXAMEN', 'AUTRE_ETUDE'] },
  { id: 'SPORT', label: 'Sport', color: '#f59e0b', subCategories: ['VOLLEYBALL', 'FOOTBALL', 'BASKETBALL', 'TENNIS', 'NATATION', 'RUNNING', 'VELO', 'MUSCULATION'] },
  { id: 'LOISIR', label: 'Loisir', color: '#ec4899', subCategories: ['CINEMA', 'THEATRE', 'MUSIQUE', 'JEUX_VIDEO', 'LECTURE', 'VOYAGE'] },
  { id: 'MENAGER', label: 'Ménager', color: '#10b981', subCategories: [] },
  { id: 'RENCONTRE', label: 'Rencontre', color: '#ef4444', subCategories: [] },
];

// Ajout de la prop initialData ici
export default function EventForm({ isOpen, onClose, onSave, initialDate, initialHour, initialData }) {
  // Fonction utilitaire pour formater la date en 'YYYY-MM-DD' (format requis par l'input date)
  const formatDateForInput = (date) => {
    if (!date) return '';
    const d = new Date(date);
    // On ajuste le décalage horaire pour rester sur le bon jour local
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  const [formData, setFormData] = useState({
    summary: '',
    description: '',
    address: '',
    category: 'TRAVAIL',
    subCategory: '',
    duration: 1,
    hour: 9,
    date: '' // Nouveau champ pour stocker la date sélectionnée
  });

  // Initialisation des données à l'ouverture (Création OU Modification)
  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        // --- CAS 1 : MODIFICATION (initialData existe) ---
        // On convertit les strings ISO en objets Date
        const startDate = new Date(initialData.startTime);
        const endDate = new Date(initialData.endTime);
        
        // Calcul de la durée en heures (différence en ms / ms_par_heure)
        // On arrondit pour correspondre aux options du select (1h, 2h...)
        let durationCalc = Math.round((endDate - startDate) / (1000 * 60 * 60));
        if (durationCalc < 1) durationCalc = 1;

        // Gestion de l'adresse : le backend peut renvoyer un objet ou une string
        let addressVal = '';
        if (initialData.location) {
            addressVal = typeof initialData.location === 'object' 
                ? initialData.location.address 
                : initialData.location;
        }

        setFormData({
          summary: initialData.summary || initialData.title || '',
          description: initialData.description || '',
          address: addressVal,
          // Si l'événement a une catégorie stockée, on l'utilise, sinon défaut
          category: initialData.category || 'TRAVAIL', 
          subCategory: initialData.subCategory || '',
          date: formatDateForInput(startDate), // On extrait YYYY-MM-DD
          hour: startDate.getHours(),          // On extrait l'heure
          duration: durationCalc
        });

      } else if (initialDate) {
        // --- CAS 2 : CRÉATION (Pas d'initialData, mais une date de clic) ---
        setFormData(prev => ({
          ...prev,
          summary: '',
          description: '',
          address: '',
          category: 'TRAVAIL',
          subCategory: '',
          // On initialise avec la date cliquée dans le calendrier
          date: formatDateForInput(initialDate),
          hour: initialHour !== undefined ? initialHour : 9,
          duration: 1
        }));
      }
    }
  }, [isOpen, initialDate, initialHour, initialData]);

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSave = () => {
    // 1. Validation : On doit avoir une date
    if (!formData.date) return;

    // 2. On découpe la chaîne "YYYY-MM-DD" pour obtenir les nombres bruts
    // Cela évite les interprétations automatiques de fuseau horaire par le navigateur
    const [year, month, day] = formData.date.split('-').map(Number);

    // 3. On crée la date de DÉBUT en heure locale stricte
    // (Mois commence à 0 en JS, donc month - 1)
    const startDateTime = new Date(year, month - 1, day, formData.hour, 0, 0);

    // 4. On crée la date de FIN en ajoutant la durée
    const endDateTime = new Date(startDateTime);
    endDateTime.setHours(startDateTime.getHours() + Number(formData.duration));

    // --- FONCTION MAGIQUE : Formate en ISO sans changer le fuseau (garde l'heure locale) ---
    const toLocalISOString = (date) => {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        const h = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');
        const s = String(date.getSeconds()).padStart(2, '0');
        // On renvoie YYYY-MM-DDTHH:mm:ss (sans le 'Z' à la fin)
        return `${y}-${m}-${d}T${h}:${min}:${s}`;
    };
    // -------------------------------------------------------------------------------------

    const selectedCategory = CATEGORIES.find(c => c.id === formData.category);

    const eventPayload = {
      summary: formData.summary,
      description: formData.description, // Ajout si le backend le supporte
      location: formData.address ? { address: formData.address } : null,
      
      // UTILISATION DE LA NOUVELLE FONCTION AU LIEU DE toISOString()
      startTime: toLocalISOString(startDateTime), 
      endTime: toLocalISOString(endDateTime),
      
      category: formData.category,
      subCategory: formData.subCategory, // Ajout
      color: selectedCategory ? selectedCategory.color : '#3b82f6'
    };

    onSave(eventPayload);
    // onClose est géré par le parent après le save généralement, mais ici ok
    // Note: Si onSave est async, il vaudrait mieux attendre, mais gardons la structure actuelle
  };

  if (!isOpen) return null;

  const currentCategory = CATEGORIES.find(c => c.id === formData.category);

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          {/* Titre dynamique */}
          <h2>{initialData ? "Modifier l'événement" : "Nouvel événement"}</h2>
          <button className="btn-icon-close" onClick={onClose}>×</button>
        </div>

        <div className="modal-body">

          {/* Description / Titre */}
          <div className="form-group">
            <label>Titre / Description</label>
            <textarea
              className="input-field textarea-field"
              value={formData.summary}
              onChange={(e) => handleChange('summary', e.target.value)}
              placeholder="Ex: Révision Maths"
              rows={3}
              autoFocus // Focus automatique à l'ouverture
            />
          </div>

          {/* Adresse (Corrigé pour éviter [object Object]) */}
          <div className="form-group">
            <label>Adresse</label>
            <GooglePlacesAutocomplete
              value={formData.address}
              onChange={(val) => {
                // Si l'utilisateur clique sur une suggestion, val est un objet { address, lat, lng }
                // Sinon (saisie manuelle), val est une string.
                const addressText = (val && typeof val === 'object') ? val.address : val;
                handleChange('address', addressText);
              }}
            />
          </div>

          {/* Catégorie */}
          <div className="form-group">
            <label>Catégorie</label>
            <select
              className="input-field"
              value={formData.category}
              onChange={(e) => handleChange('category', e.target.value)}
            >
              {CATEGORIES.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.label}</option>
              ))}
            </select>
          </div>

          {/* Sous-catégorie */}
          {currentCategory?.subCategories?.length > 0 && (
            <div className="form-group">
              <label>Type de {currentCategory.label}</label>
              <select
                className="input-field"
                value={formData.subCategory}
                onChange={(e) => handleChange('subCategory', e.target.value)}
              >
                <option value="">Sélectionner...</option>
                {currentCategory.subCategories.map(sub => (
                  <option key={sub} value={sub}>{sub}</option>
                ))}
              </select>
            </div>
          )}

          {/* Ligne Date / Heure / Durée */}
          <div className="form-row-3">
            
            {/* petit calendrier défilable */}
            <div className="form-group">
              <label>Date</label>
              <input 
                type="date" 
                className="input-field"
                value={formData.date}
                onChange={(e) => handleChange('date', e.target.value)}
                required
              />
            </div>
            
            <div className="form-group">
              <label>Heure</label>
              <select 
                className="input-field"
                value={formData.hour}
                onChange={(e) => handleChange('hour', Number(e.target.value))}
              >
                {Array.from({ length: 24 }, (_, i) => (
                  <option key={i} value={i}>{i}h00</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Durée (h)</label>
              <select 
                className="input-field"
                value={formData.duration}
                onChange={(e) => handleChange('duration', Number(e.target.value))}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8].map(h => (
                  <option key={h} value={h}>{h}h</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button className="btn-modal-cancel" onClick={onClose}>Annuler</button>
          <button 
            className="btn-modal-save" 
            onClick={handleSave} 
            disabled={!formData.summary}
          >
            {initialData ? "Modifier" : "Enregistrer"}
          </button>
        </div>
      </div>
    </div>
  );
}