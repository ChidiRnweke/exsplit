export const load = (event): ExpensePayload => {
    return {
        data: [
            {
                user: "John Doe",
                description: "Lunch",
                price: "20",
                date: "2021-08-01",
                link: idToRoute(event.route.id, "1")

            },
            {
                user: "John Doe",
                description: "Dinner",
                price: "40",
                date: "2021-08-02",
                link: idToRoute(event.route.id, "2")
            },
            {
                user: "Jane Smith",
                description: "Breakfast",
                price: "15",
                date: "2021-08-03",
                link: idToRoute(event.route.id, "3")
            },
            {
                user: "Jane Smith",
                description: "Snack",
                price: "5",
                date: "2021-08-04",
                link: idToRoute(event.route.id, "4")
            },
            {
                user: "Alice Johnson",
                description: "Coffee",
                price: "10",
                date: "2021-08-05",
                link: idToRoute(event.route.id, "5")
            }
        ]
    }
}

const idToRoute = (currentRoute: string, expenseId: string) => `${currentRoute}/${expenseId}`;

export interface Expense {
    user: string;
    description: string;
    price: string;
    date: string;
    link: string;
}

interface ExpensePayload {
    data: Expense[];
}