import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useState } from "react";
import Layout from "./components/Layout";
import Login from "./pages/Login";
import Products from "./pages/Products";
import Inventory from "./pages/Inventory";
import Orders from "./pages/Orders";
import Coupons from "./pages/Coupons";

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(
    localStorage.getItem("adminAuth") === "true",
  );

  const login = (password: string) => {
    if (password === "Admin123") {
      setIsAuthenticated(true);
      localStorage.setItem("adminAuth", "true");
      return true;
    }
    return false;
  };

  const logout = () => {
    setIsAuthenticated(false);
    localStorage.removeItem("adminAuth");
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/admin-login" element={<Login onLogin={login} />} />

        {isAuthenticated ? (
          <Route
            path="/admin-panel-secreto-123"
            element={<Layout onLogout={logout} />}
          >
            <Route index element={<Navigate to="products" replace />} />
            <Route path="products" element={<Products />} />
            <Route path="inventory" element={<Inventory />} />
            <Route path="orders" element={<Orders />} />
            <Route path="coupons" element={<Coupons />} />
          </Route>
        ) : (
          <Route
            path="/admin-panel-secreto-123/*"
            element={<Navigate to="/admin-login" replace />}
          />
        )}

        <Route
          path="*"
          element={<Navigate to="/admin-panel-secreto-123" replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}
