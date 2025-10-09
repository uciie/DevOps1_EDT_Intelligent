import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

function Home() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    const storedUser = localStorage.getItem("currentUser");
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("currentUser");
    setUser(null);
  };

  return (
    <div style={{ padding: "2rem" }}>
      <h2>Page d'accueil</h2>

      {user ? (
        <>
          <p>
            ✅ Bienvenue, <strong>{user.username}</strong> (ID : {user.id})
          </p>
          <button onClick={handleLogout}>Se déconnecter</button>
        </>
      ) : (
        <p>
          Bienvenue sur notre site&nbsp;!{" "}
          <Link to="/register">Créer un compte</Link>
        </p>
      )}
    </div>
  );
}

export default Home;
