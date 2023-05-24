package com.bolsaidead.springboot.webflux.app.controllers;

import com.bolsaidead.springboot.webflux.app.models.documents.Categoria;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import com.bolsaidead.springboot.webflux.app.models.services.ProductoService;
import com.mongodb.client.model.ReturnDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

// contendra metodos handler para el request
@SessionAttributes("producto") // el producto se guarda en la sesion http
@Controller
public class ProductoController {

    //! Leer variable de aplication properties
    @Value("${config.uploads.path}")
    private String path;

    @Autowired
    private ProductoService service;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);


    @ModelAttribute("categorias")
    public Flux<Categoria> categorias() {
        return service.findAllCategoria();
    }

    @GetMapping("/ver/{id}")
    public Mono<String> ver(Model model, @PathVariable String id) {
        return service.findById(id).doOnNext(producto -> {
                    model.addAttribute("producto", producto);
                    model.addAttribute("titulo", "Detalle Producto");
                    //default - flux
                }).switchIfEmpty(Mono.just(new Producto()))
                .flatMap(producto -> {
                    if (producto.getId() == null) {
                        //! Debemos retornar un publisher - flux o mono
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(producto);
                }).then(Mono.just("ver"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?eror=no+existe+el+producto"));

    }

    //Expresion regular para colocar la extencion de la imagen
    @GetMapping("/uploads/img/{nombreFoto:.+}")
    public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
          Path ruta = Paths.get(path).resolve(nombreFoto).toAbsolutePath();

        Resource imagen  = new UrlResource(ruta.toUri());


        return Mono.just(ResponseEntity
                        .ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\""+ imagen.getFilename() + "\"")
                .body(imagen));

    }


    @GetMapping("/listar")
    public Mono<String> listar(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUppercase();
        productoFlux.subscribe(producto -> log.info(producto.getCategoria().getNombre()));
        // Pasar datos a la vista, tendra una capa de abstraccion reactiva en el servelet
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");

        return Mono.just("listar");
    }

    //registrar
    @GetMapping("/form")
    public Mono<String> crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de producto");
        model.addAttribute("boton", "Crear");

        return Mono.just("form");
    }

    // Editar  - handler
    @GetMapping("/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model) {
        Mono<Producto> productoMono = service.findById(id).doOnNext(producto -> {
            log.info("Producto: " + producto.getNombre());
        }).defaultIfEmpty(new Producto());
        model.addAttribute("producto", productoMono);
        model.addAttribute("boton", "Editar");

        model.addAttribute("titulo", "Editar  producto");
        return Mono.just("form");
    }

    // Editar - con otro medodo reactivo - lindo la verdad
    @GetMapping("/form-v2/{id}")
    public Mono<String> editarV2(@PathVariable String id, Model model) {
        return service.findById(id).doOnNext(producto -> {
                    log.info("Producto: " + producto.getNombre());
                    model.addAttribute("boton", "Editar");

                    model.addAttribute("titulo", "Editar  producto");
                    model.addAttribute("producto", producto);
                }).defaultIfEmpty(new Producto())
                .flatMap(producto -> {
                    if (producto.getId() == null) {
                        //! Debemos retornar un publisher - flux o mono
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(producto);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?eror=no+existe+el+producto"));
    }

    @PostMapping("/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status) {

        if (result.hasErrors()) {
            model.addAttribute("titulo", "Errores en formulario producto");
            model.addAttribute("boton", "Guardar");
            return Mono.just("form");
        } else {
            status.setComplete();

            Mono<Categoria> categoria = service.findCategoriaById(producto.getCategoria().getId());

            return categoria.flatMap(c -> {
                        if (producto.getCreateAt() == null) {
                            producto.setCreateAt(new Date());
                        }

                        if (!file.filename().isEmpty()) {
                            producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "")
                            );
                        }
                        producto.setCategoria(c);
                        return service.save(producto);
                    }).doOnNext(p -> {
                        log.info("Categoria asignada: " + p.getCategoria().getNombre() + " Id Cat: " + p.getCategoria().getId());
                        log.info("Producto guardado: " + p.getNombre() + " Id: " + p.getId());
                    })
                    .flatMap(p -> {
                        if (!file.filename().isEmpty()) {
                            return file.transferTo(new File(path + p.getFoto()));
                        }
                        return Mono.empty();
                    })
                    .thenReturn("redirect:/listar?success=producto+guardado+con+exito");
        }
    }

    // Para simular delay
    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUppercase()
                .delayElements(Duration.ofSeconds(1));
        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        // Pasar datos a la vista, tendra una capa de abstraccion reactiva en el servelet, este caso solo mostrarea el flujo dde dos en dos, cada segundo
        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productoFlux, 2)); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Manejor de contra precion con chuked
    @GetMapping("/listar-full")
    public String listarFull(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUppercaseRepeat();// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Manejor de contra precion con chuked - pero a vista en especifico
    @GetMapping("/listar-chunked")
    public String listarChunked(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUppercaseRepeat();// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked"; // indica a que html se le retornara la informacion a de la vista
    }

    @GetMapping("/eliminar/{id}")
    public Mono<String> eliminar(@PathVariable String id) {

        return service.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(producto -> {
                    if (producto.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(producto);
                })
                .flatMap(producto -> {
                    log.info("Eliminado producto: " + producto.getNombre() + " | " + producto.getId());
                    return service.delete(producto);
                }).then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=producto+no+eliminado+con+exito"));
    }
}

