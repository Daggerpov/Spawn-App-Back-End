package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.user.api.dto.SearchResultUserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SearchedUserResult {
    @JsonProperty("users")
    private List<SearchResultUserDTO> users;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SearchedUserResult other = (SearchedUserResult) obj;
        return isListSame(getUsers(), other.getUsers());
    }

    private <T> boolean isListSame(List<T> list1, List<T> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            T o1 = list1.get(i);
            T o2 = list2.get(i);
            if (!o1.equals(o2)) return false;
        }
        return true;
    }
}
