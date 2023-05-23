package com.bolsaidead.springboot.webflux.app.controllers;

import com.bolsaidead.springboot.webflux.app.models.documents.Categoria;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import com.bolsaidead.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;
import java.util.Date;

// contendra metodos handler para el request
@SessionAttributes("producto") // el producto se guarda en la sesion http
@Controller
public class ProductoController {
    @Autowired
    private ProductoService service;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);


    @ModelAttribute("categorias")
    public Flux<Categoria> categorias() {
        return service.findAllCategoria();
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


    // Guardar, se enviara los datos que estan poblados en el objeto producto y se hidrata xd
    @PostMapping("/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult result, SessionStatus status, Model model) {
        //contiene todos los mensajes de los resulados, tiene que ir pegado al objeto que se esa validando
        if (result.hasErrors()) {
            model.addAttribute("boton", "Guardar");
            model.addAttribute("titulo", "Errores en el formulario producto");
            return Mono.just("form");
        } else {
            status.setComplete(); //! indica que se halla completado la sesion


            Mono<Categoria> categoria = service.findCategoriaById(producto.getCategoria().getId());


            //Transformar el flujo de producto para agregar la categoria
            return categoria.flatMap(categoriaPlus -> {

                if (producto.getCreateAt() == null) {
                    producto.setCreateAt(new Date());
                }

                producto.setCategoria(categoriaPlus);
                return service.save(producto);
            }).doOnNext(productoGuardado -> {
                log.info("Categoria asignada : " + productoGuardado.getCategoria().getNombre() + " Id:" + productoGuardado.getCategoria().getId());

                log.info("Producto guardado: " + productoGuardado.getNombre() + " Id:" + productoGuardado.getId());
            }).thenReturn("redirect:/listar?success=producto+guardo+con+exito"); // REtorna un mono string, guarda el valor de la respuesta

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

