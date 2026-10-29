package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ticket: ANIMAL-1 (see README).
 * Three tests are already implemented as a worked example (also see
 * AdopterServiceTest for the same pattern) — add the remaining tests yourself.
 */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private AnimalService animalService;

    @Captor
    private ArgumentCaptor<Animal> animalCaptor;

    @Captor
    private ArgumentCaptor<List<Long>> idsCaptor;

    @Test
    void create_shouldSaveAnimalWithAvailableStatus() {
        Animal saved = new Animal();
        saved.setId(1L);
        saved.setName("Rex");
        saved.setType(AnimalType.DOG);
        saved.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.save(any(Animal.class))).thenReturn(saved);

        AnimalResponse response = animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.findById(99L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adopt_shouldThrowWhenAnimalAlreadyAdopted() {
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(1L, "Anna", "anna@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adopt_shouldAdoptAnimalAndSendNotification() {
        Animal availableAnimal = new Animal();
        availableAnimal.setId(1L);
        availableAnimal.setName("Doggy");
        availableAnimal.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(availableAnimal));
        when(animalRepository.save(any(Animal.class))).thenReturn(availableAnimal);

        AnimalResponse response = animalService.adopt(new AdoptionRequest(1L, "Mykolas", "mykolas@mail.com"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(AnimalStatus.ADOPTED);

        verify(animalRepository).save(animalCaptor.capture());
        assertThat(animalCaptor.getValue().getStatus()).isEqualTo(AnimalStatus.ADOPTED);

        verify(notificationClient).sendAdoptionNotification(1L, "Doggy", "mykolas@mail.com");
    }

    @Test
    void reserve_shouldNotifyWithExactIds() {
        Animal reserved = new Animal();
        reserved.setId(1L);
        reserved.setName("Rex");
        reserved.setType(AnimalType.DOG);
        reserved.setStatus(AnimalStatus.AVAILABLE);

        Animal reserved2 = new Animal();
        reserved2.setId(2L);
        reserved2.setName("Catito");
        reserved2.setType(AnimalType.CAT);
        reserved2.setStatus(AnimalStatus.AVAILABLE);

        Animal reserved3 = new Animal();
        reserved3.setId(3L);
        reserved3.setName("Rabbito");
        reserved3.setType(AnimalType.RABBIT);
        reserved3.setStatus(AnimalStatus.AVAILABLE);

        List<Long> ids = List.of(reserved.getId(), reserved2.getId(), reserved3.getId());

        when(animalRepository.findAllById(ids)).thenReturn(List.of(reserved, reserved2, reserved3));
        when(animalRepository.save(any(Animal.class))).thenAnswer(returnsFirstArg());

        List<AnimalResponse> responses = animalService.reserveMultiple(ids);

        verify(notificationClient).sendBulkStatusNotification(idsCaptor.capture(), eq("RESERVED"));
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(reserved.getId(), reserved2.getId(), reserved3.getId());
        assertThat(responses)
                .extracting(AnimalResponse::status)
                .containsOnly(AnimalStatus.RESERVED);

    }

    @Test
    void create_shouldRejectDuplicateMicrochipIdsWithoutSaving(){
        when(animalRepository.existsByMicrochipId("chip-1")).thenReturn(true);

        assertThatThrownBy(() -> animalService.create(
                new AnimalCreateRequest("Doggy", AnimalType.DOG, "Chichiacha", 3, "Bites", "chip-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chip-1");

        verify(animalRepository, never()).save(any(Animal.class));
        verifyNoInteractions(notificationClient);
    }
}
