CREATE
  TABLE wager_round
  (
    wager_round_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    exchange_rate_id BIGINT NOT NULL,
    PRIMARY KEY (wager_round_id)
  );

PARTITION TABLE wager_round ON COLUMN wager_round_id;

CREATE
  TABLE wager_state
  (
    wager_round_id BIGINT NOT NULL,
    wager_id BIGINT NOT NULL,
    state TINYINT NOT NULL,
    amount BIGINT NOT NULL,
    created TIMESTAMP NOT NULL,
    PRIMARY KEY (wager_round_id, wager_id, state)
  );

PARTITION TABLE wager_state ON COLUMN wager_round_id;


-- Roles
CREATE ROLE system WITH sysproc,adhoc,defaultproc;
-- Not great in a way to allow sysproc for app, but it simplifies some things
CREATE ROLE app WITH sysproc;
CREATE ROLE test WITH sysproc,adhoc,defaultproc;

-- Procedures
CREATE PROCEDURE ALLOW app FROM CLASS com.williamsinteractive.casino.wager.procedures.RecordTransition;
PARTITION PROCEDURE RecordTransition ON TABLE wager_round COLUMN wager_round_id;

CREATE PROCEDURE WAGER_ROUND_SELECT_ALL ALLOW test AS SELECT * FROM wager_round;
CREATE PROCEDURE WAGER_STATE_SELECT_ALL ALLOW test AS SELECT * FROM wager_state;
