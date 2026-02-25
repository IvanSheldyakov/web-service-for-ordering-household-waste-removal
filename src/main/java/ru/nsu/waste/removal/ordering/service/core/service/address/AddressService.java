package ru.nsu.waste.removal.ordering.service.core.service.address;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.repository.address.AddressRepository;
import ru.nsu.waste.removal.ordering.service.core.model.address.AddressCreationData;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public long add(AddressCreationData addressCreationData) {
        return addressRepository.add(addressCreationData);
    }
}

