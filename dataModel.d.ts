// dataModel.d.ts

// Type definitions for various data models in CreditRepairAI

declare module "CreditRepairAI" {
    export type Dispute = {
        id: string;
        status: string;
        createdAt: Date;
    };
    
    export type User = {
        id: string;
        name: string;
        email: string;
    };
}