package team.phase2.data.universities;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/universities")
public class UniversitiesController {
  private final JdbcTemplate jdbc;
  public UniversitiesController(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  @GetMapping
  public List<Map<String,Object>> getAll(){
    return jdbc.queryForList("SELECT * FROM universities");
  }

  @GetMapping("/{id}")
  public Map<String,Object> getById(@PathVariable long id){
    return jdbc.queryForMap("SELECT * FROM universities WHERE id = ?", id);
  }

  @GetMapping("/by-country-name/{country}")
  public List<Map<String,Object>> byCountry(@PathVariable String country){
    return jdbc.queryForList("SELECT * FROM universities WHERE UPPER(country)=UPPER(?)", country);
  }

  @GetMapping("/search")
  public List<Map<String,Object>> search(@RequestParam(required=false, name="domain_suffix") String suffix){
    if (suffix == null || suffix.isBlank()) return getAll();
    return jdbc.queryForList("SELECT * FROM universities WHERE LOWER(domain) LIKE LOWER(?)", "%"+suffix);
  }
}

