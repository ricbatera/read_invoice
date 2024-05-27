package br.com.dgconsult.reader_invoice.api.contoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.dgconsult.reader_invoice.domain.service.ReaderInvoiceService;
import org.springframework.web.bind.annotation.GetMapping;


@CrossOrigin
@RestController
@RequestMapping("/reader")
public class ReaderInvoiceController {

    @Autowired
    private ReaderInvoiceService service;

    @GetMapping("/processa")
    public String processaBoletos() {
        service.contaBoletos();
        return new String("Sucesso!");
    }    
    
}
