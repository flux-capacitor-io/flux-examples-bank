package io.fluxcapacitor.clientapp.adapter.authentication.tokenfilters;

import io.fluxcapacitor.clientapp.adapter.authentication.TokenFilter;
import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class CompositeTokenFilter implements TokenFilter {
    private final List<TokenFilter> delegates;

    public CompositeTokenFilter(TokenFilter... tokenFilters) {
        delegates = Arrays.stream(tokenFilters).collect(Collectors.toList());
    }

    @Override
    public Optional<AuthenticationToken> obtainToken(Map<String, List<String>> headers) {
        Optional<AuthenticationToken> result = Optional.empty();
        for (TokenFilter delegate : delegates) {
            result = delegate.obtainToken(headers);
            if (result.isPresent()) {
                return result;
            }
        }
        return result;
    }
}
