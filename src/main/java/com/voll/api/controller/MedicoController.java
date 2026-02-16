package com.voll.api.controller;

import com.voll.api.domain.medico.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;


@RestController
@RequestMapping("/medicos")
public class MedicoController {
    @Autowired
    private MedicoRepository medicoRepository;

    @Transactional
    @PostMapping
    public ResponseEntity registrar(@RequestBody @Valid DatosRegistroMedico datos, UriComponentsBuilder uriComponentsBuilder) {
       var medico = new Medico(datos);
       medicoRepository.save(medico);
       var uri = uriComponentsBuilder.path("/medicos/{id}").buildAndExpand(medico.getId()).toUri();
       return ResponseEntity.created(uri).body(new DatosDetalleMedico(medico));

    }
// para listar una opcion
//    @GetMapping("/listar")
//    public List<DatosListaMedico> listar(){
//        return medicoRepository.findAll().
//                stream()
//                .map(m -> new DatosListaMedico(m.getNombre(),m.getEmail(),m.getDocumento(),m.getEspecialidad()))
//                .limit(10)
//                .collect(Collectors.toList());
//    }
//@GetMapping
//public List<DatosListaMedico> listar(){
//    return medicoRepository.findAll().
//            stream()
//            .map(DatosListaMedico::new)
//            .limit(10)
//            .toList();
//
//}
    @GetMapping //@PageableDefault(size=10, sort={"nombre"}) parametro
    public ResponseEntity<Page<DatosListaMedico>> listar(Pageable paginacion){
        var page = medicoRepository.findAllByActivoTrue(paginacion)
                .map(DatosListaMedico::new);
        return ResponseEntity.ok(page);

    }

    @Transactional
    @PutMapping
    public ResponseEntity actualizar(@RequestBody @Valid DatosActualizarMedico datos){
        var medico = medicoRepository.getReferenceById(datos.id());
        medico.actualizarInformaciones(datos);
        return ResponseEntity.ok(new DatosDetalleMedico(medico));
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity eliminar(@PathVariable Long id){
        var medico = medicoRepository.getReferenceById(id);
        medico.eliminacionLogica();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity detallar( @PathVariable Long id){
        var medico = medicoRepository.getReferenceById(id);
        return ResponseEntity.ok(new DatosListaMedico(medico));
    }


}
