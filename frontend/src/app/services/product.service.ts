import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Product, ProductDetailDTO, ProductPriceDTO } from '../models/product';
import { Observable } from 'rxjs';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  
  products = signal<Product[]>([]);

  constructor() {
    this.loadProducts();
  }

  loadProducts() {
    this.http.get<Page<Product>>(`http://localhost:8080/products?size=100`)
      .subscribe({
        next: (res) => this.products.set(res.content),
        error: (err) => console.error('Error loading products', err)
      });
  }

  getAll(page = 0, size = 10, sort = 'name,asc'): Observable<Page<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    return this.http.get<Page<Product>>(`http://localhost:8080/products`, { params });
  }

  getProduct(id: string): Observable<Product> {
    return this.http.get<Product>(`http://localhost:8080/products/${id}`);
  }

  getProductDetails(id: string, quantity?: number): Observable<ProductDetailDTO> {
    let params = new HttpParams();
    if (quantity !== undefined) {
      params = params.set('quantity', quantity.toString());
    }
    return this.http.get<ProductDetailDTO>(`http://localhost:8080/products/${id}/details`, { params });
  }

  searchProducts(name: string): Observable<Product[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<Product[]>(`http://localhost:8080/products/search`, { params });
  }

  filterByCategory(category: string, page = 0, size = 10): Observable<Page<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<Product>>(`http://localhost:8080/products/category/${category}`, { params });
  }

  filterByPrice(min: number, max: number, page = 0, size = 10): Observable<Page<Product>> {
    const params = new HttpParams()
      .set('min', min.toString())
      .set('max', max.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<Product>>(`http://localhost:8080/products/price-range`, { params });
  }

  getPopular(page = 0, size = 10): Observable<Page<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<Page<Product>>(`http://localhost:8080/products/popular`, { params });
  }

  getBulkPrices(productIds: string[]): Observable<ProductPriceDTO[]> {
    return this.http.post<ProductPriceDTO[]>(`http://localhost:8080/products/bulk-prices`, productIds);
  }
}
