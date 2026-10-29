package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.*;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService adoptionEligibilityService;

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenAdopterNotFound() {
        when(adopterRepository.findById(1L)).thenReturn(Optional.empty());

        AdoptionResult adoptionResult = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(adoptionResult.approved()).isFalse();
        assertThat(adoptionResult.reason()).isEqualTo(RejectionReasons.ADOPTER_NOT_FOUND);
        verifyNoInteractions(animalRepository, auditLogger, notificationClient);
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenAnimalNotFound() {
        Adopter adopter = validAdopter();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.empty());

        AdoptionResult adoptionResult = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(adoptionResult.approved()).isFalse();
        assertThat(adoptionResult.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_FOUND);
        verifyNoInteractions(auditLogger, notificationClient);


    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenAnimalNotAvailable() {
        Animal adoptedAnimal = animal();
        adoptedAnimal.setStatus(AnimalStatus.ADOPTED);

        Adopter validAdopter = validAdopter();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(validAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(adoptedAnimal));

        AdoptionResult adoptionResult = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(adoptionResult.approved()).isFalse();
        assertThat(adoptionResult.reason()).isEqualTo(RejectionReasons.ANIMAL_NOT_AVAILABLE);
        verifyNoInteractions(auditLogger, notificationClient);
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenAdopterIsUnderaged() {
        Adopter underAgedAdopter = validAdopter();
        underAgedAdopter.setAge(17);
        Animal validAnimal = animal();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(underAgedAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(validAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.UNDERAGE);
        verify(auditLogger).logRejection(1L, 1L, RejectionReason.UNDERAGE);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApprove_whenAdopterIs18() {
        Adopter adopter = validAdopter();
        adopter.setAge(18);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal()));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenPetLimitIsReachedWithSmallPropertyAnd3Pets() {
        Adopter smallPropertyAdopter = validAdopter();
        smallPropertyAdopter.setLargeProperty(false);
        smallPropertyAdopter.setCurrentPetCount(3);
        Animal validAnimal = animal();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(smallPropertyAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(validAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(1L, 1L, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }
    @Test
    void evaluateAdoption_shouldApprove_whenSmallPropertyHas2Pets() {
        Adopter smallPropertyAdopter = validAdopter();
        smallPropertyAdopter.setLargeProperty(false);
        smallPropertyAdopter.setCurrentPetCount(2);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(smallPropertyAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal()));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenPetLimitIsReachedWithLargePropertyAnd5Pets() {
        Adopter largePropertyAdopter = validAdopter();
        largePropertyAdopter.setLargeProperty(true);
        largePropertyAdopter.setCurrentPetCount(5);
        Animal validAnimal = animal();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(largePropertyAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(validAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(1L, 1L, RejectionReason.PET_LIMIT_REACHED);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApprove_whenLargePropertyHas4Pets() {
        Adopter largePropertyAdopter = validAdopter();
        largePropertyAdopter.setLargeProperty(true);
        largePropertyAdopter.setCurrentPetCount(4);
        Animal validAnimal = animal();

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(largePropertyAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(validAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenBirdAndAdopterHasNoPermit() {
        Adopter noPermitAdopter = validAdopter();
        noPermitAdopter.setExoticPermit(false);
        Animal exoticAnimal = animal();
        exoticAnimal.setType(AnimalType.BIRD);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(noPermitAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(exoticAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        verify(auditLogger).logRejection(1L, 1L, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldRejectAdoption_whenRabbitAndAdopterHasNoPermit() {
        Adopter noPermitAdopter = validAdopter();
        noPermitAdopter.setExoticPermit(false);
        Animal exoticAnimal = animal();
        exoticAnimal.setType(AnimalType.RABBIT);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(noPermitAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(exoticAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        verify(auditLogger).logRejection(1L, 1L, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApprove_whenExoticAnimalAndPermit() {
        Adopter permitAdopter = validAdopter();
        permitAdopter.setExoticPermit(true);
        Animal exoticAnimal = animal();
        exoticAnimal.setType(AnimalType.BIRD);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(permitAdopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(exoticAnimal));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isTrue();
    }

    @Test
    void calculatePriorityScore_shouldAddAllBonuses() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(0);
        adopter.setPreviousAdoptions(4);
        adopter.setLargeProperty(true);
        Animal senior = animal();
        senior.setAge(8);

        int score = adoptionEligibilityService.calculatePriorityScore(adopter, senior);

        assertThat(score).isEqualTo(10 + 5 + 15 + 20);
    }

    @Test
    void calculatePriorityScore_shouldNeverBeNegative() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(30);
        adopter.setPreviousAdoptions(15);//10+5-(30*2)=-45
        adopter.setLargeProperty(false);

        assertThat(adoptionEligibilityService.calculatePriorityScore(adopter, animal()))
                .isEqualTo(0);
    }

    @Test
    void evaluateAdoption_shouldApproveWithScore_logApproval_andNotify() {
        Adopter adopter = validAdopter();
        adopter.setPreviousAdoptions(1);

        when(adopterRepository.findById(1L)).thenReturn(Optional.of(adopter));
        when(animalRepository.findById(1L)).thenReturn(Optional.of(animal()));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(1L, 1L);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(6); //10-2*2
        verify(auditLogger).logApproval(1L, 1L, 6);
        verify(notificationClient).sendApprovalNotification("mykolas@gmail.com", "Doggy");
    }

    @Test
    void calculatePriorityScore_shouldNotAddSeniorBonus_whenAnimalAgeUnknown() {
        Animal unknownAgeAnimal = animal();
        unknownAgeAnimal.setAge(null);

        assertThat(adoptionEligibilityService.calculatePriorityScore(validAdopter(), unknownAgeAnimal))
                .isEqualTo(0);
    }

    @Test
    void calculatePriorityScore_shouldNotAddExtraBonus_whenThreePreviousAdoptions() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(0);
        adopter.setPreviousAdoptions(3);

        assertThat(adoptionEligibilityService.calculatePriorityScore(adopter, animal()))
                .isEqualTo(10);
    }

    @Test
    void calculatePriorityScore_shouldNotAddSeniorBonus_whenAnimalIs7() {
        Adopter adopter = validAdopter();
        adopter.setCurrentPetCount(0);
        Animal animal = animal();
        animal.setAge(7);

        assertThat(adoptionEligibilityService.calculatePriorityScore(adopter, animal))
                .isEqualTo(0);
    }



    private Adopter validAdopter() {
        Adopter validAdopter = new Adopter();
        validAdopter.setId(1L);
        validAdopter.setName("Mykolas");
        validAdopter.setEmail("mykolas@gmail.com");
        validAdopter.setAge(27);
        validAdopter.setCurrentPetCount(2);
        return validAdopter;
    }

    private Animal animal() {
        Animal validAnimal = new Animal();
        validAnimal.setId(1L);
        validAnimal.setName("Doggy");
        validAnimal.setType(AnimalType.DOG);
        validAnimal.setAge(3);
        validAnimal.setStatus(AnimalStatus.AVAILABLE);
        return validAnimal;
    }


}