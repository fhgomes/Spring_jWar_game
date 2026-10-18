package br.com.bnuuy.jwar.server.api;

import br.com.bnuuy.jwar.server.auth.CurrentUser;
import br.com.bnuuy.jwar.server.dto.AddTroopsRequest;
import br.com.bnuuy.jwar.server.dto.AttackRequest;
import br.com.bnuuy.jwar.server.dto.AttackResponse;
import br.com.bnuuy.jwar.server.dto.ExchangeCardsRequest;
import br.com.bnuuy.jwar.server.dto.ExchangeCardsResponse;
import br.com.bnuuy.jwar.server.dto.GameStateSnapshot;
import br.com.bnuuy.jwar.server.dto.MatchSummary;
import br.com.bnuuy.jwar.server.dto.MoveTroopsRequest;
import br.com.bnuuy.jwar.server.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Matches", description = "In-progress and historical matches")
public class MatchController {

    private final MatchService matchService;
    private final CurrentUser currentUser;

    @Operation(summary = "Snapshot of the current public game state")
    @GetMapping("/api/matches/{id}")
    public GameStateSnapshot get(@PathVariable("id") UUID matchId) {
        return matchService.getSnapshot(matchId, currentUser.userId());
    }

    @Operation(summary = "Add troops to a country owned by the caller during the ADD_TROOPS phase")
    @PostMapping("/api/matches/{id}/add-troops")
    public GameStateSnapshot addTroops(@PathVariable("id") UUID matchId,
                                       @Valid @RequestBody AddTroopsRequest request) {
        return matchService.addTroops(matchId, currentUser.userId(), request.countryCode(), request.qty());
    }

    @Operation(summary = "Attack an enemy country during the ATTACK phase")
    @PostMapping("/api/matches/{id}/attack")
    public AttackResponse attack(@PathVariable("id") UUID matchId,
                                 @Valid @RequestBody AttackRequest request) {
        return matchService.attack(matchId, currentUser.userId(),
            request.srcCountryCode(), request.tgtCountryCode());
    }

    @Operation(summary = "Move troops between own contiguous territories during the MOVE_TROOPS phase")
    @PostMapping("/api/matches/{id}/move-troops")
    public GameStateSnapshot moveTroops(@PathVariable("id") UUID matchId,
                                        @Valid @RequestBody MoveTroopsRequest request) {
        return matchService.moveTroops(matchId, currentUser.userId(),
            request.srcCountryCode(), request.tgtCountryCode(), request.qty());
    }

    @Operation(summary = "End the current phase; from MOVE_TROOPS this also advances the turn")
    @PostMapping("/api/matches/{id}/end-phase")
    public GameStateSnapshot endPhase(@PathVariable("id") UUID matchId) {
        return matchService.endPhase(matchId, currentUser.userId());
    }

    @Operation(summary = "Exchange a set of country cards for bonus troops during the ADD_TROOPS phase")
    @PostMapping("/api/matches/{id}/exchange-cards")
    public ExchangeCardsResponse exchangeCards(@PathVariable("id") UUID matchId,
                                               @Valid @RequestBody ExchangeCardsRequest request) {
        return matchService.exchangeCards(matchId, currentUser.userId(), request.cardCodes());
    }

    @Operation(summary = "List the caller's matches (in-progress and historical)")
    @GetMapping("/api/me/matches")
    public List<MatchSummary> myMatches() {
        return matchService.listMyMatches(currentUser.userId());
    }
}
