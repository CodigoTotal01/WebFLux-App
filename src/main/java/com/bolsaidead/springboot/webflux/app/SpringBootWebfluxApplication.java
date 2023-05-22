package com.bolsaidead.springboot.webflux.app;

import com.bolsaidead.springboot.webflux.app.models.dao.ProductoDao;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import javax.xml.crypto.Data;
import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

    @Autowired
    private ProductoDao dao;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Eliminar la tabla - productos
        mongoTemplate.dropCollection("productos").subscribe();

        Flux.just(new Producto("TV Panasonic Pantalla LCD", 456.89),
                new Producto("Sony camara HD digital", 786.23 ),
                new Producto("Play Station 3",700.00 ),
                new Producto("Laptop lenovo decima generacion", 5000.00),
                new Producto("Disco duro externo de 5T", 689.97),
                new Producto("Teclado mecanico redragon", 350.87 ),
                new Producto("Galaxy fold 3v",3500.99 )
                )
                //Asi retornara nadamas (save) un flujo de mono - osea otro flujo
                .flatMap(producto -> {
                    producto.setCreateAt(new Date());
                    return dao.save(producto);
                })
                // no tenemos el objeto en si, si no el mono, en estos casos el flatmap obtiene el obserbable y lo aplana a nuestro objeto deseado
                .subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre()));
    }





}
