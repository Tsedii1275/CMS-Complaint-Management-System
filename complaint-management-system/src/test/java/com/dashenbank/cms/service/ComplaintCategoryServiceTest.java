package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintCategory;
import com.dashenbank.cms.repository.ComplaintCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintCategoryServiceTest {

    @Mock
    private ComplaintCategoryRepository repository;

    @InjectMocks
    private ComplaintCategoryService service;

    @Test
    void newCategoryIsStoredAndSelectedByItsDisplayName() {
        when(repository.findByNameKey("card pickup delay")).thenReturn(Optional.empty());
        when(repository.findTopByOrderBySortOrderDesc()).thenReturn(Optional.of(existing("Branch Operation", true, true, 11)));
        when(repository.save(any(ComplaintCategory.class))).thenAnswer(invocation -> {
            ComplaintCategory row = invocation.getArgument(0);
            row.setId(20L);
            return row;
        });

        var saved = service.create("  Card   Pickup Delay ");

        ArgumentCaptor<ComplaintCategory> captor = ArgumentCaptor.forClass(ComplaintCategory.class);
        verify(repository).save(captor.capture());
        assertEquals("Card Pickup Delay", captor.getValue().getName());
        assertEquals("card pickup delay", captor.getValue().getNameKey());
        assertFalse(captor.getValue().isBuiltin());
        assertEquals(12, captor.getValue().getSortOrder());
        assertEquals("Card Pickup Delay", saved.get("name"));
    }

    @Test
    void duplicateActiveNameIsRejected() {
        when(repository.findByNameKey("transaction error")).thenReturn(Optional.of(existing("Transaction Error", true, true, 2)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create("transaction error"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ComplaintCategoryService.ALREADY_EXISTS, ex.getReason());
    }

    @Test
    void turnedOffNameIsNotAddedAgain() {
        when(repository.findByNameKey("merchant fees")).thenReturn(Optional.of(existing("Merchant Fees", false, false, 12)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create("Merchant Fees"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(ComplaintCategoryService.TURNED_OFF, ex.getReason());
    }

    @Test
    void standardCategoryCannotBeTurnedOff() {
        when(repository.findById(1L)).thenReturn(Optional.of(existing("Branch Operation", true, true, 11)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.setActive(1L, false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(ComplaintCategoryService.STANDARD_STAYS, ex.getReason());
    }

    @Test
    void addedCategoryCanBeTurnedOff() {
        ComplaintCategory added = existing("Merchant Fees", true, false, 12);
        added.setId(20L);
        when(repository.findById(20L)).thenReturn(Optional.of(added));
        when(repository.save(added)).thenReturn(added);

        var saved = service.setActive(20L, false);

        assertEquals(false, saved.get("active"));
        assertFalse(added.isActive());
    }

    private static ComplaintCategory existing(String name, boolean active, boolean builtin, int sortOrder) {
        return ComplaintCategory.builder()
                .name(name)
                .nameKey(ComplaintCategoryService.nameKey(name))
                .active(active)
                .builtin(builtin)
                .sortOrder(sortOrder)
                .build();
    }
}
