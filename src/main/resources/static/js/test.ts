const obj = { a: "test1", b: "test2", c: "test3"} as const;
type key = typeof obj[keyof typeof obj];

interface Props {
    key: number,
    value: any
}


const isRejected = (input: PromiseSettledResult<unknown>): input is PromiseRejectedResult =>
    input.status === 'rejected'

const isFulfilled = <T>(input: PromiseSettledResult<T>): input is PromiseFulfilledResult<T> =>
    input.status === 'fulfilled'

const myPromise = async () => Promise.resolve("hello world");

const data = await Promise.allSettled([myPromise()]);

const response = data.find(isFulfilled)?.value
const error = data.find(isRejected)?.reason