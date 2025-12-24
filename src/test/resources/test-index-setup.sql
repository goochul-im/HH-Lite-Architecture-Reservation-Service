CREATE UNIQUE INDEX uk_active_reservation ON reservation (reservation_date, seat_num,
(CASE WHEN reservation_status in ('RESERVE', 'PENDING') THEN reservation_status ELSE NULL END));
