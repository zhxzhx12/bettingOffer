import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export let options = {
    stages: [
        { duration: '1m', target: 100 }, // ramp up to 100 users
        { duration: '8m', target: 100 }, // stay at 100 users for 5 minutes
        { duration: '1m', target: 0 },   // ramp down to 0 users
    ],
};

const BASE_URL = 'http://localhost:8001';
const NUM_STAKES = 100;
const MAX_STAKE = 100000;

let stakes = Array.from({ length: NUM_STAKES }, () => Math.floor(Math.random() * MAX_STAKE) );
stakes[Math.floor(Math.random() * NUM_STAKES)] = MAX_STAKE ;

export default function () {
    let betofferid = Math.floor(Math.random() * 999000) + 1000;
    let customerId = Math.floor(Math.random() * 400) + 100;

    let getSession = http.get(`${BASE_URL}/${customerId}/session`);
    check(getSession, { 'session status is 200': (r) => r.status === 200 });

    let sessionKey = getSession.body;

    stakes.forEach(stake => {
        let postStake = http.post(`${BASE_URL}/${betofferid}/stake?sessionkey=${sessionKey}`, JSON.stringify(stake), {
            headers: { 'Content-Type': 'application/json' },
        });

        if (postStake.status === 401) {
            getSession = http.get(`${BASE_URL}/${customerId}/session`);
            check(getSession, { 'session status is 200': (r) => r.status === 200 });

            sessionKey = getSession.body;

            postStake = http.post(`${BASE_URL}/${betofferid}/stake?sessionkey=${sessionKey}`, JSON.stringify(stake), {
                headers: { 'Content-Type': 'application/json' },
            });
        }

        check(postStake, { 'postStake status is 200': (r) => r.status === 200 });
    });

    let getHightStakes = http.get(`${BASE_URL}/${betofferid}/highstakes`);
    check(getHightStakes, { 'getHightStakes status is 200': (r) => r.status === 200 });

    let csvBody = getHightStakes.body;
    check(csvBody, { 'getHightStakes response is not empty': (r) => r.length > 0 });

    let highstakes = csvBody.split(',');
    check(highstakes, { 'getHightStakes has 20 entries': (r) => r.length <= 20 });

    let prevStake = Number.MAX_VALUE;
    let maxFound = false;

    highstakes.forEach(entry => {
        let parts = entry.split('=');
        let stake = parseInt(parts[1], 10);

        check(stake, { 'stake is valid': (s) => s >= 0 && s <= MAX_STAKE });

        if (stake === MAX_STAKE) {
            maxFound = true;
        }

        check(stake, { 'stake is in descending order': (s) => s <= prevStake });
        prevStake = stake;
    });

    check(maxFound, { 'max stake found': (m) => m === true });

    // sleep(1);
}