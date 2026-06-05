import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { randomUUID } from 'crypto';
import {
  buildPaginatedResult,
  clampLimit,
  cursorFilter,
} from '../common/pagination/cursor.util';
import { upsertByClientId } from '../common/mongo/client-id-upsert';
import { mapDoc, mapDocs } from '../database/document.util';
import { Debt, DebtDocument } from '../database/schemas/debt.schema';
import { Transaction, TransactionDocument } from '../database/schemas/transaction.schema';
import { CreateDebtDto, CreatePenaltyDto, UpdateDebtDto } from './dto/create-debt.dto';
import { CreateTransactionDto } from './dto/create-transaction.dto';

@Injectable()
export class FinancesService {
  constructor(
    @InjectModel(Transaction.name)
    private readonly transactionModel: Model<TransactionDocument>,
    @InjectModel(Debt.name) private readonly debtModel: Model<DebtDocument>,
  ) {}

  async listTransactions(
    type?: string,
    category?: string,
    startDate?: string,
    endDate?: string,
    limit?: string,
    cursor?: string,
  ) {
    const filter: Record<string, unknown> = { deletedAt: null };
    if (type) filter.type = type;
    if (category) filter.category = category;
    if (startDate || endDate) {
      filter.date = {};
      if (startDate) {
        (filter.date as Record<string, Date>).$gte = new Date(startDate);
      }
      if (endDate) {
        (filter.date as Record<string, Date>).$lte = new Date(endDate);
      }
    }
    const pageLimit = clampLimit(limit);
    if (cursor || limit) {
      const docs = await this.transactionModel
        .find(cursorFilter(filter, cursor))
        .sort({ updatedAt: 1, _id: 1 })
        .limit(pageLimit + 1)
        .exec();
      const mapped = mapDocs(docs);
      return buildPaginatedResult(
        mapped,
        pageLimit,
        (i) => i.id as string,
        (i) => new Date(i.updatedAt as string),
      );
    }
    const items = await this.transactionModel
      .find(filter)
      .sort({ date: -1 })
      .exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  async createTransaction(dto: CreateTransactionDto) {
    const doc = await upsertByClientId(
      this.transactionModel,
      dto.id,
      {
        type: dto.type,
        amount: dto.amount,
        category: dto.category,
        description: dto.description ?? null,
        date: new Date(dto.date),
        linkedTaskId: dto.linkedTaskId ?? null,
        updatedAt: new Date(dto.updatedAt),
        deletedAt: null,
      },
      new Date(dto.createdAt),
    );
    return mapDoc(doc)!;
  }

  async createPenaltyEgress(dto: CreatePenaltyDto) {
    const now = new Date();
    const id = randomUUID();
    const doc = await this.transactionModel.create({
      _id: id,
      type: 'egress',
      amount: dto.amount,
      category: 'Penalty',
      description: dto.description ?? `Task skip penalty for ${dto.taskId}`,
      date: now,
      linkedTaskId: dto.taskId,
      createdAt: now,
      updatedAt: now,
      deletedAt: null,
    });
    return mapDoc(doc)!;
  }

  async listDebts(resolved?: string, limit?: string, cursor?: string) {
    const filter: Record<string, unknown> = { deletedAt: null };
    if (resolved === 'true') filter.isResolved = true;
    else if (resolved === 'false') filter.isResolved = false;
    const pageLimit = clampLimit(limit);
    if (cursor || limit) {
      const docs = await this.debtModel
        .find(cursorFilter(filter, cursor))
        .sort({ updatedAt: 1, _id: 1 })
        .limit(pageLimit + 1)
        .exec();
      const mapped = mapDocs(docs);
      return buildPaginatedResult(
        mapped,
        pageLimit,
        (i) => i.id as string,
        (i) => new Date(i.updatedAt as string),
      );
    }
    const items = await this.debtModel.find(filter).sort({ updatedAt: -1 }).exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  async createDebt(dto: CreateDebtDto) {
    const doc = await upsertByClientId(
      this.debtModel,
      dto.id,
      {
        contactName: dto.contactName,
        direction: dto.direction,
        totalAmount: dto.totalAmount,
        remainingAmount: dto.totalAmount,
        dueDate: dto.dueDate ? new Date(dto.dueDate) : null,
        notes: dto.notes ?? null,
        isResolved: false,
        updatedAt: new Date(dto.updatedAt),
        deletedAt: null,
      },
      new Date(dto.createdAt),
    );
    return mapDoc(doc)!;
  }

  async updateDebt(id: string, dto: UpdateDebtDto) {
    const existing = await this.debtModel.findById(id).exec();
    if (!existing || existing.deletedAt) {
      throw new NotFoundException({
        code: 'DEBT_NOT_FOUND',
        message: `Debt ${id} not found`,
      });
    }
    const doc = await this.debtModel
      .findOneAndUpdate(
        { _id: id },
        {
          ...(dto.remainingAmount !== undefined && { remainingAmount: dto.remainingAmount }),
          ...(dto.isResolved !== undefined && { isResolved: dto.isResolved }),
          ...(dto.notes !== undefined && { notes: dto.notes }),
          updatedAt: new Date(dto.updatedAt),
        },
        { new: true },
      )
      .exec();
    return mapDoc(doc)!;
  }

  async getSummary(year: number, month: number) {
    const start = new Date(Date.UTC(year, month - 1, 1));
    const end = new Date(Date.UTC(year, month, 1));

    const transactions = await this.transactionModel
      .find({
        deletedAt: null,
        date: { $gte: start, $lt: end },
      })
      .exec();

    let ingress = 0;
    let egress = 0;
    for (const tx of transactions) {
      if (tx.type === 'ingress') ingress += tx.amount;
      else egress += tx.amount;
    }

    const unresolvedDebts = await this.debtModel
      .find({ deletedAt: null, isResolved: false })
      .exec();

    let owedToMe = 0;
    let owedByMe = 0;
    for (const debt of unresolvedDebts) {
      if (debt.direction === 'owed_to_me') owedToMe += debt.remainingAmount;
      else owedByMe += debt.remainingAmount;
    }

    return {
      ingress,
      egress,
      netBalance: ingress - egress,
      owedToMe,
      owedByMe,
    };
  }
}
