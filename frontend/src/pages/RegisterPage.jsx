import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../api/userApi";

function RegisterPage() {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Appel API centralisé
      const userId = await registerUser(formData);

      // Construit un objet utilisateur simple pour stockage local
      const user = { id: userId, username: formData.username };
      localStorage.setItem("currentUser", JSON.stringify(user));

      // Redirection immédiate vers la page d’accueil
      navigate("/", { replace: true });
    } catch (err) {
      alert("Erreur lors de la création du compte. Vérifiez les informations saisies.");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: "2rem" }}>
      <h1>Créer un compte</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: "1rem" }}>
          <label>Nom d'utilisateur :</label><br />
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
            style={{ width: "250px", padding: "8px" }}
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <label>Mot de passe :</label><br />
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            style={{ width: "250px", padding: "8px" }}
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? "Création en cours..." : "Créer le compte"}
        </button>
      </form>
    </div>
  );
}

export default RegisterPage;
