import { Outlet, NavLink } from "react-router-dom";
import {
  Package,
  ShoppingCart,
  Tag,
  LogOut,
  LayoutDashboard,
  Archive,
} from "lucide-react";

export default function Layout({ onLogout }: { onLogout: () => void }) {
  const navItems = [
    {
      to: "products",
      icon: <Package className="w-5 h-5" />,
      label: "Products",
    },
    {
      to: "inventory",
      icon: <Archive className="w-5 h-5" />,
      label: "Inventory",
    },
    {
      to: "orders",
      icon: <ShoppingCart className="w-5 h-5" />,
      label: "Orders",
    },
    { to: "coupons", icon: <Tag className="w-5 h-5" />, label: "Coupons" },
  ];

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 font-sans">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <LayoutDashboard className="w-6 h-6 text-indigo-600 mr-2" />
          <span className="text-lg font-bold text-gray-800">Admin Panel</span>
        </div>

        <nav className="flex-1 overflow-y-auto py-4">
          <ul className="space-y-1 px-3">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center px-3 py-2.5 rounded-lg transition-colors ${
                      isActive
                        ? "bg-indigo-50 text-indigo-700 font-medium"
                        : "text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                    }`
                  }
                >
                  {item.icon}
                  <span className="ml-3">{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <div className="p-4 border-t border-gray-200">
          <button
            onClick={onLogout}
            className="flex items-center w-full px-3 py-2 text-gray-600 rounded-lg hover:bg-red-50 hover:text-red-600 transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span className="ml-3 font-medium">Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto p-8">
        <Outlet />
      </main>
    </div>
  );
}
