import { useEffect, useState } from 'react';
import api from './services/api';

function App() {
  const [message, setMessage] = useState('');

  useEffect(() => {
    api.get('/hello').then(res => setMessage(res.data));
  }, []);

  return (
    <div style={{ textAlign: 'center', marginTop: '50px' }}>
      <h1>Frontend React</h1>
      <p>{message}</p>
    </div>
  );
}

export default App;
