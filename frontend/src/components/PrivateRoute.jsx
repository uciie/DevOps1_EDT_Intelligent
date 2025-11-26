import { Navigate } from "react-router-dom";
import { isAuthenticated } from "../api/authApi";

/**
 * Composant qui protège les routes nécessitant une authentification
 * Redirige vers /login si l'utilisateur n'est pas connecté
 */
function PrivateRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  return children;
}

export default PrivateRoute;