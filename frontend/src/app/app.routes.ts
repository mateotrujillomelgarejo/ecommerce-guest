import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./pages/catalog/catalog').then(m => m.CatalogComponent) },
  { path: 'cart', loadComponent: () => import('./pages/cart/cart').then(m => m.CartComponent) },
  { path: 'checkout', loadComponent: () => import('./pages/checkout/checkout').then(m => m.CheckoutComponent) },
  { path: 'product/:id', loadComponent: () => import('./pages/product/product').then(m => m.ProductComponent) },
  { path: 'success', loadComponent: () => import('./pages/success/success').then(m => m.SuccessComponent) },
  { path: '**', redirectTo: '' }
];
