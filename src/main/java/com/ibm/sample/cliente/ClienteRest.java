package com.ibm.sample.cliente;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentracing.Span;

import com.ibm.sample.PropagacaoContexto;
import com.ibm.sample.cliente.dto.Cliente;
import com.ibm.sample.cliente.dto.RetornoCliente;
import com.ibm.sample.cliente.jpa.ClienteRepository;

@Controller
@RestController
public class ClienteRest extends PropagacaoContexto {

	@Autowired
	private ClienteRepository clienteJpa;
	Logger logger = LoggerFactory.getLogger(ClienteRest.class);
	
	
    @Autowired
    MeterRegistry registry;

	@Autowired
	Counter contadorRequperaCliente;

	@Autowired
	Counter contadorPesquisaClientes;

	@Autowired
	Counter contadorExcluiClientes;

	@Autowired
	Counter contadorCadastroClientes;

	@GetMapping("/cliente/pesquisa/{nome}")
	public List<Cliente> recuperaClientes(@PathVariable String nome, HttpServletRequest request)
	{
		Timer.Sample timer = Timer.start(registry);
		logger.debug("[recuperaClientes] " + nome);
		Span span = this.startServerSpan("consultaBaseDados", request);
		List<Cliente> lista = clienteJpa.findByNome(nome);
		logger.debug("Found: " + lista.size() + " customer that starts a name with " + nome);
		contadorPesquisaClientes.increment();
		span.finish();
		timer.stop(registry.timer("app.duration", "app", "cliente-rest", "funcao", "recuperaClientes"));
		return lista;
	}

	
	@DeleteMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> excluirCliente(@PathVariable Long cpf, HttpServletRequest request)
	{
		Timer.Sample timer = Timer.start(registry);
		logger.debug("[excluirCliente] " + cpf);
		Span span = this.startServerSpan("exclusaoClienteBaseDados", request);
		span.setTag("cpf", cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
		RetornoCliente retorno = new RetornoCliente();
		if (!cliente.isPresent())
		{
			logger.info("Customer not found to be delete with this ID: " + cpf);
			span.finish();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
		}
		else
		{
			Cliente cli = cliente.get();
			retorno.setCliente(cli);
			logger.debug("Sending SQl instructions to tha database to delete the record of the customer with ID: " + cli.toString());
			clienteJpa.delete(cli);
			retorno.setMensagem( "Customer deleted!");
			logger.debug("Customer deleted successfully " + cli.toString());
			contadorExcluiClientes.increment();
			retorno.setCodigo("202-Deleted");
		}
		span.finish();
		timer.stop(registry.timer("app.duration", "app", "cliente-rest", "funcao", "excluirCliente"));
		return ResponseEntity.ok(retorno);
	}
	
	@GetMapping("/cliente/{cpf}")
	public ResponseEntity<RetornoCliente> recuperaCliente(@PathVariable Long cpf, HttpServletRequest request)
	{
		
		Timer.Sample timer = Timer.start(registry);
		logger.debug("[recuperaCliente] " + cpf);
		Span span = this.startServerSpan("consultaBaseDados", request);
		span.setTag("cpf", cpf);
		Optional<Cliente> cliente= clienteJpa.findById(cpf);
		contadorRequperaCliente.increment();
		RetornoCliente retorno = new RetornoCliente();
		if (!cliente.isPresent())
		{
			logger.info("Customer not found with this ID: " + cpf);
			span.log( "Customer not found with the ID suplied");
			span.finish();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		else
		{
			retorno.setCliente(cliente.get());
			logger.debug("Customer Found: " + retorno.getCliente().toString());
			retorno.setMensagem( "Customer Found!");
			retorno.setCodigo("200-FOUND");
			span.setTag("name", retorno.getCliente().getNome());
		}
		span.finish();
		timer.stop(registry.timer("app.duration", "app", "cliente-rest", "funcao", "recuperaCliente"));
		return ResponseEntity.ok(retorno);
	}
	
	@PutMapping("/cliente")
	public ResponseEntity<RetornoCliente> atualizaCliente( @RequestBody Cliente cliente, HttpServletRequest request)
	{
		Timer.Sample timer = Timer.start(registry);
		logger.debug("[atualizaClientes] ");
		Span span = this.startServerSpan("atualizacaoClienteBaseDados", request);
		try
		{
			
			logger.debug("It will validate the customer data before update in the database!");
			span.log("It will validate the customer data before update in the database!");
			/*if (cliente.getCpf()==234455)
			{
				throw new Exception("CPF is a required field");
			}*/
			validaCliente(span,cliente);
			span.setTag("cpf", cliente.getCpf());
			span.setTag("nome", cliente.getNome());
			span.setTag("nome", cliente.getNome());
			span.setTag("mae", cliente.getMae());
			span.setTag("logradouro", cliente.getLogradouro());
			span.setTag("numero", cliente.getNumero());
			span.setTag("complemento", cliente.getComplemento());
			span.setTag("cep", cliente.getCep());
			span.setTag("cidade", cliente.getCidade());
			span.setTag("uf", cliente.getUf());
			span.setTag("nascimento", cliente.getNasc().toString());
			logger.debug("Data validated successfully!");
			logger.debug("It will validate if the customer exists in the database");
			Span spanConsulta = tracer.buildSpan("consultaBaseMySQL").asChildOf(span).start();
			spanConsulta.setTag("sql", "select * from cliente where cpf = ? ");
			spanConsulta.setTag("cpf", cliente.getCpf());
			Optional<Cliente> clienteConsulta= clienteJpa.findById(cliente.getCpf());
			spanConsulta.finish();
			RetornoCliente retorno = new RetornoCliente();

			
			if (!clienteConsulta.isPresent())
			{
				span.log("Does not exist customer with this ID in the database");
				logger.info("Does not exist customer with this ID in the database: " + cliente.getCpf());
				retorno.setCliente(clienteConsulta.get());
				retorno.setMensagem( "Does not exist customer with this ID in the database");
				retorno.setCodigo("404-CLIENT NOT FOUND");
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			Span spanGravacao = tracer.buildSpan("atualizacaoBaseMysql").asChildOf(span).start();
			spanGravacao.setTag("cpf", cliente.getCpf());
			spanGravacao.setTag("nome", cliente.getNome());
			spanGravacao.setTag("mae", cliente.getMae());
			spanGravacao.setTag("logradouro", cliente.getLogradouro());
			spanGravacao.setTag("numero", cliente.getNumero());
			spanGravacao.setTag("complemento", cliente.getComplemento());
			spanGravacao.setTag("cep", cliente.getCep());
			spanGravacao.setTag("cidade", cliente.getCidade());
			spanGravacao.setTag("uf", cliente.getUf());
			spanGravacao.setTag("nascimento", cliente.getNasc().toString());
			clienteJpa.save(cliente);
			spanGravacao.finish();
			logger.info("Customer data updated successfully! " + cliente.toString());

			retorno.setCliente(cliente);
			retorno.setMensagem( "Customer data updated successfully!");
			contadorCadastroClientes.increment();
			retorno.setCodigo("202-ACCEPTED");
			
			return ResponseEntity.ok(retorno);
		}
		catch (Exception e)
		{
			span.setTag("error",true);
			span.setTag("errorMessage", e.getMessage());
			logger.error("Error to update Customer data " + e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		finally
		{
			timer.stop(registry.timer("app.duration", "app", "cliente-rest", "funcao", "atualizaCliente"));
			span.finish();
		}

	}

	@PostMapping("/cliente")
	public ResponseEntity<RetornoCliente> incluiCliente(@RequestBody Cliente cliente, HttpServletRequest request)
	{
		Timer.Sample timer = Timer.start(registry);
		logger.debug("[incluiCliente] ");
		Span span = this.startServerSpan("inclusaoClienteBaseDados", request);
		try
		{
			
			logger.debug("It will validate the customer data before insert into the database!");
			validaCliente(span,cliente);
			span.setTag("cpf", cliente.getCpf());
			span.setTag("nome", cliente.getNome());
			span.setTag("nome", cliente.getNome());
			span.setTag("mae", cliente.getMae());
			span.setTag("logradouro", cliente.getLogradouro());
			span.setTag("numero", cliente.getNumero());
			span.setTag("complemento", cliente.getComplemento());
			span.setTag("cep", cliente.getCep());
			span.setTag("cidade", cliente.getCidade());
			span.setTag("uf", cliente.getUf());
			span.setTag("nascimento", cliente.getNasc().toString());
			logger.debug("Data validated successfully!");
			
			logger.debug("It will validate if the customer exists in the database");
			Span spanConsulta = tracer.buildSpan("consultaBaseMySQL").asChildOf(span).start();
			spanConsulta.setTag("sql", "select * from cliente where cpf = ? ");
			spanConsulta.setTag("cpf", cliente.getCpf());
			Optional<Cliente> clienteConsulta= clienteJpa.findById(cliente.getCpf());
			spanConsulta.finish();
			RetornoCliente retorno = new RetornoCliente();

			
			if (clienteConsulta.isPresent())
			{
				span.log("Customer already exists with this ID: " + cliente.getCpf());
				logger.info("Customer already exists with this ID: " + cliente.getCpf());
				retorno.setCliente(clienteConsulta.get());
				retorno.setMensagem( "Customer already exists with this ID!");
				retorno.setCodigo("208-CLIENT EXIST");
				return new ResponseEntity<>(HttpStatus.FOUND);
			}
			Span spanGravacao = tracer.buildSpan("gravacaoBaseMysql").asChildOf(span).start();
			spanGravacao.setTag("sql", "insert into cliente values (?,?,?,?,?,?,?,?,?,?) ");
			spanGravacao.setTag("cpf", cliente.getCpf());
			spanGravacao.setTag("nome", cliente.getNome());
			spanGravacao.setTag("mae", cliente.getMae());
			spanGravacao.setTag("logradouro", cliente.getLogradouro());
			spanGravacao.setTag("numero", cliente.getNumero());
			spanGravacao.setTag("complemento", cliente.getComplemento());
			spanGravacao.setTag("cep", cliente.getCep());
			spanGravacao.setTag("cidade", cliente.getCidade());
			spanGravacao.setTag("uf", cliente.getUf());
			spanGravacao.setTag("nascimento", cliente.getNasc().toString());
			clienteJpa.save(cliente);
			spanGravacao.finish();
			logger.info("Customer data included into database successfuly! " + cliente.toString());

			retorno.setCliente(cliente);
			retorno.setMensagem( "Customer created!");
			contadorCadastroClientes.increment();
			retorno.setCodigo("201-CREATED");
			timer.stop(registry.timer("app.duration", "app", "cliente-rest", "funcao", "incluiCliente"));
			return ResponseEntity.ok(retorno);
		}
		catch (Exception e)
		{
			span.setTag("error",true);
			span.setTag("errorMessage", e.getMessage());
			logger.error("Error to add a new client: " + e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		finally
		{
			span.finish();
		}
	}
	
	private void validaCliente(Span spanPai, Cliente cliente) throws Exception
	{
		Span span = tracer.buildSpan("validaDadosCliente").asChildOf(spanPai).start();
		if (cliente==null)
		{
			span.setTag("errorMessage", "Payload is invalid, it wasn't found customer data");
			span.setTag("error", true);
			span.finish();
			throw new Exception("Payload is invalid, it wasn't found customer data");
		}
		if (cliente.getCpf()==null || cliente.getCpf()==0)
		{
			span.setTag("errorMessage", "CPF is a requeried field");
			span.setTag("error", true);
			span.finish();
			throw new Exception("CPF is a required field");
		}
		if (cliente.getNome()==null || cliente.getNome().length()==0)
		{
			span.setTag("errorMessage", "Nome is a required field");
			span.setTag("error", true);
			span.finish();
			throw new Exception("Nome is a required field");
		}
		span.log("Customer data validated successfully!");
		span.finish();
	}
}
