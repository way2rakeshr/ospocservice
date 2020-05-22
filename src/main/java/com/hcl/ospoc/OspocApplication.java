package com.hcl.ospoc;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
public class OspocApplication {
	public static void main(String[] args) {
		SpringApplication.run(OspocApplication.class, args);
	}

}

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("api/orders")
@Slf4j
class OrderController {
	@Value("${osp.token}")
	private String token;

	@Value("${osp.url}")
	private String ospUrl;

	@Autowired
	private OrderRepository orderRepository;

	@GetMapping
	public Page<Order> all(Pageable pageable) {
		return orderRepository.findAll(pageable);
	}

	@GetMapping("/{id}")
	public Order one(@PathVariable Long id) throws NotFoundException {
		log.info("Getting order - {}", id);
		return orderRepository.findById(id).orElseThrow(() -> new NotFoundException());
	}

	@PostMapping
	public Order createOder(@RequestBody Order newOrder) throws Exception {
		log.info("Creating an order");
		Order order = orderRepository.save(newOrder);
		NamespaceRequest namespaceRequest = new NamespaceRequest();
		namespaceRequest.setMetadata(new Metadata(newOrder.getProjectName()));
		namespaceRequest.setApiVersion("v1");
		namespaceRequest.setKind("ProjectRequest");
		/* calling openshit platform api to create namespace */
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("Authorization", token);
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(namespaceRequest), headers);
		ResponseEntity<String> response = restTemplate.exchange(ospUrl, HttpMethod.POST, request, String.class);
		if (!(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED)) {
			throw new Exception("Failed to call openshit platform");
		}
		log.info(response.getBody());
		return order;
	}

	@PutMapping("/{id}")
	public Order replaceOrder(@RequestBody Order newOrder, @PathVariable Long id) {
		log.info("Updating order - {}", id);
		return orderRepository.findById(id).map(order -> {
			order.setProjectName(newOrder.getProjectName());
			order.setProjectDisplayName(newOrder.getProjectDisplayName());
			order.setProjectDescription(newOrder.getProjectDescription());
			order.setProjectAdminUser(newOrder.getProjectAdminUser());
			order.setProjectRequestingUser(newOrder.getProjectRequestingUser());
			order.setEnvironment(order.getEnvironment());
			return orderRepository.save(order);
		}).orElseGet(() -> {
			newOrder.setId(id);
			return orderRepository.save(newOrder);
		});
	}

	@DeleteMapping("/{id}")
	public void deleteEmployee(@PathVariable Long id) {
		log.info("Deleting order - {}", id);
		orderRepository.deleteById(id);
	}
}

interface OrderRepository extends JpaRepository<Order, Long> {
}

@Entity
@NoArgsConstructor
@Data
@Table(name = "ORDERS")
class Order {
	@Id
	@GeneratedValue
	Long id;
	String projectName;
	String projectDisplayName;
	String projectDescription;
	String projectAdminUser;
	String projectRequestingUser;
	String environment;
	String businessUnit;
	String costCode;

	Order(String projectName, String environment, String projectDisplayName, String projectDescription,
			String projectAdminUser, String projectRequestingUser, String businessUnit, String costCode) {
		this.projectName = projectName;
		this.environment = environment;
		this.projectAdminUser = projectAdminUser;
		this.projectDisplayName = projectDisplayName;
		this.projectDescription = projectDescription;
		this.projectRequestingUser = projectRequestingUser;
		this.businessUnit = businessUnit;
		this.costCode = costCode;
	}

}

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
class NamespaceRequest {
	String kind;
	String apiVersion;
	Metadata metadata;
}

@Data
@ToString
@AllArgsConstructor
class Metadata {
	String name;
}