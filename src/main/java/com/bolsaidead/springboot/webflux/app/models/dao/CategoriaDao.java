package com.bolsaidead.springboot.webflux.app.models.dao;

import com.bolsaidead.springboot.webflux.app.models.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String> {
}
