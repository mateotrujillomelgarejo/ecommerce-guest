const BASE_URL = "http://localhost:8080";

export const api = {
  // Products
  getProducts: async () => {
    const res = await fetch(`${BASE_URL}/products?size=100`);
    if (!res.ok) throw new Error("Error fetching products");
    return res.json();
  },
  createProduct: async (product: any) => {
    const res = await fetch(`${BASE_URL}/products`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(product),
    });
    if (!res.ok) throw new Error("Error creating product");
    return res.json();
  },
  updateProduct: async (id: string, product: any) => {
    const res = await fetch(`${BASE_URL}/products/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(product),
    });
    if (!res.ok) throw new Error("Error updating product");
    return res.json();
  },
  deleteProduct: async (id: string) => {
    const res = await fetch(`${BASE_URL}/products/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error("Error deleting product");
  },

  // Inventory
  getInventory: async () => {
    const res = await fetch(`${BASE_URL}/inventory/all`);
    if (!res.ok) throw new Error("Error fetching inventory");
    return res.json();
  },
  createInventory: async (inventory: any) => {
    const res = await fetch(`${BASE_URL}/inventory/save`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(inventory),
    });
    if (!res.ok) throw new Error("Error creating inventory");
    return res.json();
  },
  updateInventory: async (productId: string, quantity: number) => {
    const res = await fetch(
      `${BASE_URL}/inventory/${productId}?quantity=${quantity}`,
      { method: "PUT" },
    );
    if (!res.ok) throw new Error("Error updating inventory");
  },

  // Orders
  getOrders: async () => {
    const res = await fetch(`${BASE_URL}/orders/all`);
    if (!res.ok) throw new Error("Error fetching orders");
    return res.json();
  },
  updateOrderStatus: async (id: string, status: string) => {
    const res = await fetch(
      `${BASE_URL}/orders/${id}/status?status=${status}`,
      { method: "PATCH" },
    );
    if (!res.ok) throw new Error("Error updating order status");
  },

  // Coupons
  getCoupons: async () => {
    const res = await fetch(`${BASE_URL}/pricing/admin/coupons`);
    if (!res.ok) throw new Error("Error fetching coupons");
    return res.json();
  },
  createCoupon: async (coupon: any) => {
    const res = await fetch(`${BASE_URL}/pricing/admin/coupons`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(coupon),
    });
    if (!res.ok) throw new Error("Error creating coupon");
    return res.json();
  },
};
