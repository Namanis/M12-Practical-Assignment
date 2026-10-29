package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    void findRecentAdoptionsByAnimalType_shouldFilterByTypeAndDate(){
        LocalDateTime since = LocalDateTime.of(2026, 1, 1, 0, 0);
        Animal dog = entityManager.persist(newAnimal("Rex", AnimalType.DOG));
        Animal cat = entityManager.persist(newAnimal("Mia", AnimalType.CAT));

        entityManager.persist(newRecord(dog, "recent-dog@example.com", since.plusDays(3)));//pass
        entityManager.persist(newRecord(dog, "boundary-dog@example.com", since));//pass
        entityManager.persist(newRecord(dog, "old-dog@example.com", since.minusDays(10)));//not pass
        entityManager.persist(newRecord(cat, "recent-cat@example.com", since.plusDays(20)));//cat

        entityManager.flush();
        entityManager.clear();

        List<AdoptionRecord> result = adoptionRecordRepository.findRecentAdoptionsByAnimalType(AnimalType.DOG, since);

        assertThat(result)
                .extracting(AdoptionRecord::getAdopterEmail)
                .containsExactlyInAnyOrder("recent-dog@example.com", "boundary-dog@example.com");
        assertThat(result)
                .extracting(record -> record.getAnimal().getType())
                .containsOnly(AnimalType.DOG);
    }

    private Animal newAnimal(String name, AnimalType type) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(AnimalStatus.ADOPTED);
        return animal;
    }

    private AdoptionRecord newRecord(Animal animal, String adopterEmail, LocalDateTime adoptedAt) {
        AdoptionRecord record = new AdoptionRecord();
        record.setAnimal(animal);
        record.setAdopterEmail(adopterEmail);
        record.setAdoptedAt(adoptedAt);
        return record;
    }
}