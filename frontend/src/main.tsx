import React, { Component, ErrorInfo, ReactNode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

interface Props { children: ReactNode; }
interface State { hasError: boolean; error: Error | null; }

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('GridWeaver ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 40, color: '#ff9aaa', fontFamily: 'system-ui, sans-serif', background: '#07111f', minHeight: '100vh' }}>
          <h2 style={{ color: '#ff5d73', marginTop: 0 }}>GridWeaver UI Diagnostic Alert</h2>
          <p style={{ color: '#aac0d5' }}>An unexpected UI error occurred during dashboard rendering:</p>
          <pre style={{ background: '#0e2033', padding: 16, borderRadius: 8, overflow: 'auto', color: '#f0f6fc', border: '1px solid #203e5c' }}>
            {this.state.error?.toString()}
            {'\n'}
            {this.state.error?.stack}
          </pre>
          <button
            onClick={() => {
              try {
                sessionStorage.clear();
                localStorage.clear();
              } catch {}
              window.location.href = window.location.origin + window.location.pathname + '?nocache=' + Date.now();
            }}
            style={{ background: '#1c507a', color: '#fff', padding: '10px 22px', border: 'none', borderRadius: 8, cursor: 'pointer', marginTop: 15, fontWeight: 600 }}
          >
            Hard Reload Dashboard (Clear Cache)
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
