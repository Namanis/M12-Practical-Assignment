package lv.bootcamp.shelter;


import jakarta.persistence.EntityManager;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

//BONUS
@SpringBootTest
@Transactional
public class AdoptionIntegrationTest {

    @Autowired
    private AnimalService animalService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private NotificationClient notificationClient;

    @Test
    void adoptionFlow_shouldPersistStatusAndNotify() {
        AnimalResponse animal = createAvailableAnimal();

        assertThat(animal.id()).isNotNull();
        assertThat(animal.status()).isEqualTo(AnimalStatus.AVAILABLE);

        AnimalResponse adopted = animalService.adopt(new AdoptionRequest(animal.id(), "Mykolas", "mykolas@example.com"));

        assertThat(adopted.status()).isEqualTo(AnimalStatus.ADOPTED);

        entityManager.flush();
        entityManager.clear();

        Animal persistedAnimal = animalRepository.findById(animal.id()).orElseThrow();

        assertThat(persistedAnimal.getStatus()).isEqualTo(AnimalStatus.ADOPTED);
        verify(notificationClient).sendAdoptionNotification(animal.id(), "Doggy", "mykolas@gmail.com");
    }

    @Test
    void adoptionFlow_shouldRejectSecondAdoptionAndNotifyOnlyOnce() {
        AnimalResponse animal = createAvailableAnimal();
        AnimalResponse adopted = animalService.adopt(new AdoptionRequest(animal.id(), "Mykolas", "mykolas@gmail.com"));
        assertThat(adopted.status()).isEqualTo(AnimalStatus.ADOPTED);
        verify(notificationClient).sendAdoptionNotification(animal.id(), "Doggy", "mykolas@mail.com");

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(animal.id(), "Lee", "Lee@gmail.com")))
                .isInstanceOf(IllegalStateException.class);


        verify(notificationClient, times(1)).sendAdoptionNotification(anyLong(), anyString(), anyString());
    }

    private AnimalResponse createAvailableAnimal() {
        return animalService.create(new AnimalCreateRequest("Doggy", AnimalType.DOG, "Chichuacha", 3, "Happy", null));
    }
}
