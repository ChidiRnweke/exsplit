
export const load = (event): CirclePayload => {
    const circleId = event.params.circle;
    return {
        data:
            [
                {
                    id: idToRoute(circleId, "1"),
                    title: "Noteworthy technology acquisitions 2021",
                    description: "Here are the biggest enterprise technology acquisitions of 2021 so far, in reverse chronological order.",
                },
                {
                    id: idToRoute(circleId, "2"),
                    title: "Noteworthy technology acquisitions 2021",
                    description: "Here are the biggest enterprise technology acquisitions of 2021 so far, in reverse chronological order.",
                },
            ]
    }
};

const idToRoute = (circleId: string, expenseListId: string) => `/circles/${circleId}/${expenseListId}`;

export interface CircleData {

    title: string;
    description: string;
    id: string;
}

interface CirclePayload {
    data: CircleData[];
}