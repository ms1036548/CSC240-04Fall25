package team.phase2.data.countries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/countries")
public class CountriesController {
  private final JdbcTemplate jdbc;
  public CountriesController(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  // GET /countries → return all rows
  @GetMapping
  public List<Map<String,Object>> getAll(){
    return jdbc.queryForList("SELECT * FROM countries");
  }

  // GET /countries/{id} → return one country by its ID
  @GetMapping("/{id}")
  public Map<String,Object> getById(@PathVariable long id){
    return jdbc.queryForMap("SELECT * FROM countries WHERE id = ?", id);
  }

  // GET /countries/by-code2/{code2} → return country by ISO 2-letter code
  @GetMapping("/by-code2/{code2}")
  public Map<String,Object> getByCode2(@PathVariable String code2){
    return jdbc.queryForMap("SELECT * FROM countries WHERE UPPER(code2)=UPPER(?)", code2);
  }

  // GET /countries/by-name/{name} → return country by name
  @GetMapping("/by-name/{name}")
  public Map<String,Object> getByName(@PathVariable String name){
    return jdbc.queryForMap("SELECT * FROM countries WHERE UPPER(name)=UPPER(?)", name);
  }
}

