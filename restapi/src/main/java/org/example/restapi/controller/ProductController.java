package org.example.restapi.controller;

import java.util.List;

import org.example.restapi.Product;
import org.example.restapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductService service;



    @PostMapping("/add")
    public Product addProduct(@RequestBody Product product) {
        return service.saveProduct(product);
    }



    @GetMapping("/all")
    public List<Product> findAllProducts() {
        return service.getProducts();
    }



    @GetMapping("/find/{id}")
    public Product findProductById(@PathVariable int id) {
        return service.getProductById(id);
    }



    @PutMapping("/update")
    public Product updateProduct(@RequestBody Product product) {
        return service.updateProduct(product);
    }



    @DeleteMapping("/delete/{id}")
    public String deleteProduct(@PathVariable int id) {
        return service.deleteProduct(id);
    }
}



/*
 * Endpoints
Method		Endpoint					Purpose
POST		/api/v1/products/add		Add Product
GET			/api/v1/products/all		Get All Products
GET			/api/v1/products/find/1		Get Product By ID
PUT			/api/v1/products/update		Update Product
DELETE		/api/v1/products/delete/1	Delete Product
 */
