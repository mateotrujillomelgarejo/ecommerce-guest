export interface Product {
  id: string;
  name: string;
  price: number;
  description: string;
  category: string;
  images: string[];
  averageRating?: number;
  tags?: string[];
  active?: boolean;
}

export interface ProductDetailDTO {
  product: Product;
  available: boolean;
  stockMessage: string;
}

export interface ProductPriceDTO {
  id: string;
  price: number;
}

export interface CartItem {
  productId: string;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
  imageUrl?: string;
}

export interface CartResponse {
  id: string;
  items: CartItem[];
  subtotal: number;
  discount: number;
  tax: number;
  shippingEstimate: number;
  total: number;
}

export interface Order {
  orderId: string;
  guestId: string;
  items: {
    productId: string;
    quantity: number;
    price: number;
    productName?: string;
    imageUrl?: string;
  }[];
  totalAmount: number;
  paymentId: string;
  status: string;
  createdAt: string;
  trackingNumber: string;
}
