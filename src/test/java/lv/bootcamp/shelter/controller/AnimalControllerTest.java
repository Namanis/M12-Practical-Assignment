package lv.bootcamp.shelter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.bootcamp.shelter.config.SecurityConfig;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.service.AnimalNotFoundException;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Ticket: ANIMAL-2 (see README).
 * findById_shouldReturn404WhenNotFound is already implemented as a worked
 * example (also see AdopterControllerTest for the same pattern) — add the
 * remaining tests yourself.
 * <p>
 *
 * @Import(SecurityConfig.class) is required: without it, this slice falls
 * back to Spring Boot's default "require auth for everything" security
 * instead of this project's actual (permissive, for these endpoints) rules.
 */
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void findById_shouldReturn404WhenNotFound() throws Exception {
        when(animalService.findById(99L)).thenThrow(new AnimalNotFoundException(99L));

        mockMvc.perform(get("/api/animals/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_shouldListAllAnimalsAsJSON() throws Exception {
        when(animalService.findAll()).thenReturn(List.of(
                new AnimalResponse(1L, "Doggy", AnimalType.DOG, "Hound", 3, "Happy", AnimalStatus.AVAILABLE),
                new AnimalResponse(2L, "Catty", AnimalType.CAT, "HomeCat", 2, "Angry", AnimalStatus.ADOPTED)));

        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Doggy"))
                .andExpect(jsonPath("$[0].type").value("DOG"))
                .andExpect(jsonPath("$[0].breed").value("Hound"))
                .andExpect(jsonPath("$[0].age").value(3))
                .andExpect(jsonPath("$[0].description").value("Happy"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Catty"))
                .andExpect(jsonPath("$[1].type").value("CAT"))
                .andExpect(jsonPath("$[1].breed").value("HomeCat"))
                .andExpect(jsonPath("$[1].age").value(2))
                .andExpect(jsonPath("$[1].description").value("Angry"))
                .andExpect(jsonPath("$[1].status").value("ADOPTED"));
    }

    @Test
    void create_shouldBeRejectedBeforeServiceIfBodyIsInvalid() throws Exception {
        AnimalCreateRequest request = new AnimalCreateRequest("", null, "Labrador", 3, "Friendly", null);

        mockMvc.perform(post("/api/animals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(animalService);
    }
}
