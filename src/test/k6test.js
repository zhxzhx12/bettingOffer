import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export let options = {
    stages: [
        { duration: '1m', target: 100 }, // ramp up to 100 users
        { duration: '15m', target: 100 }, // stay at 100 users for 5 minutes
        { duration: '1m', target: 0 },   // ramp down to 0 users
    ],
};

const BASE_URL = 'http://localhost:8001';
const NUM_CUSTOMERS = 100;
const NUM_STAKES = 100;
const MAX_STAKE = 100000;

let stakes = Array.from({ length: NUM_STAKES }, () => Math.floor(Math.random() * MAX_STAKE) + 100);
stakes[Math.floor(Math.random() * NUM_STAKES)] = MAX_STAKE;

let customers = Array.from({ length: NUM_CUSTOMERS }, () => Math.floor(Math.random() * 400) + 100);

export default function () {
    let betofferid = Math.floor(Math.random() * 999000) + 1000;
    let customerId = Math.floor(Math.random() * 400) + 100;

    let sessionResponse = http.get(`${BASE_URL}/${customerId}/session`);
    check(sessionResponse, { 'status is 200': (r) => r.status === 200 });

    let sessionKey = sessionResponse.body;

    stakes.forEach(stake => {
        let postResponse = http.post(`${BASE_URL}/${betofferid}/stake?sessionkey=${sessionKey}`, JSON.stringify(stake), {
            headers: { 'Content-Type': 'application/json' },
        });

        if (postResponse.status === 401) {
            sessionResponse = http.get(`${BASE_URL}/${customerId}/session`);
            check(sessionResponse, { 'status is 200': (r) => r.status === 200 });

            sessionKey = sessionResponse.body;

            postResponse = http.post(`${BASE_URL}/${betofferid}/stake?sessionkey=${sessionKey}`, JSON.stringify(stake), {
                headers: { 'Content-Type': 'application/json' },
            });
        }

        check(postResponse, { 'status is 200': (r) => r.status === 200 });
    });

    let response = http.get(`${BASE_URL}/${betofferid}/highstakes`);
    check(response, { 'status is 200': (r) => r.status === 200 });

    let csvBody = response.body;
    check(csvBody, { 'response is not empty': (r) => r.length > 0 });

    let entries = csvBody.split(',');
    check(entries, { 'has 20 entries': (r) => r.length === 20 });

    let prevStake = Number.MAX_VALUE;
    let maxFound = false;

    entries.forEach(entry => {
        let parts = entry.split('=');
        let stake = parseInt(parts[1], 10);

        check(stake, { 'stake is valid': (s) => s >= 100 && s <= MAX_STAKE });

        if (stake === MAX_STAKE) {
            maxFound = true;
        }

        check(stake, { 'stake is in descending order': (s) => s <= prevStake });
        prevStake = stake;
    });

    check(maxFound, { 'max stake found': (m) => m === true });

    sleep(1);
}