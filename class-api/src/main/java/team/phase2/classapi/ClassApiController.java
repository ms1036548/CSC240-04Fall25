package team.phase2.classapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ClassApiController {
    private final RestTemplate http;
    private final String dataBase;

    public ClassApiController(RestTemplate http, @Value("${data.api.base-url}") String dataBase) {
        this.http = http;
        this.dataBase = dataBase;
    }

    @GetMapping("/health")
    public Map<String,Object> health() {
        return Map.of("status","ok","dataApi",dataBase);
    }

    @GetMapping("/countries")
    public List<?> countries() {
        return http.getForObject(dataBase + "/countries", List.class);
    }

    @GetMapping("/universities")
    public List<?> universities() {
        return http.getForObject(dataBase + "/universities", List.class);
    }

    @GetMapping("/dashboard")
    public Map<String,Object> dashboard() {
        List<?> c = http.getForObject(dataBase + "/countries", List.class);
        List<?> u = http.getForObject(dataBase + "/universities", List.class);
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("countryCount", c == null ? 0 : c.size());
        out.put("universityCount", u == null ? 0 : u.size());
        out.put("sampleCountries", clip(c, 3));
        out.put("sampleUniversities", clip(u, 3));
        return out;
    }

    private static List<?> clip(List<?> l, int n){
        if(l == null) return List.of();
        return l.subList(0, Math.min(n, l.size()));
    }
}

