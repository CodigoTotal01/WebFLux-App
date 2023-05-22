package com.bolsaidead.springboot.webflux.app.models.dao;

import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
//Ya no es jpa, si nom reactive - ya que pose objetos mono
public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {

}
